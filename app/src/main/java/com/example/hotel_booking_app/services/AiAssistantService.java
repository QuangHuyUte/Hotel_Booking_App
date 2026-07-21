package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.RoomType;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiAssistantService {
    private static final int MAX_RESULTS = 6;

    private final CabinService cabinService;
    private final RoomTypeService roomTypeService;
    private final GeminiQueryService geminiQueryService;
    private final BookingService bookingService;

    public AiAssistantService() {
        cabinService = new CabinService();
        roomTypeService = new RoomTypeService();
        geminiQueryService = new GeminiQueryService();
        bookingService = new BookingService();
    }

    public void searchHotels(String message, SupabaseCallback<AiSearchResult> callback) {
        AiSearchQuery fallbackQuery = parseQuery(message);
        geminiQueryService.refineQuery(message, fallbackQuery, new SupabaseCallback<AiSearchQuery>() {
            @Override
            public void onSuccess(AiSearchQuery query) {
                searchWithQuery(query, callback);
            }

            @Override
            public void onError(String message) {
                searchWithQuery(fallbackQuery, callback);
            }
        });
    }

    private void searchWithQuery(AiSearchQuery query, SupabaseCallback<AiSearchResult> callback) {
        cabinService.getCabins(new SupabaseCallback<List<Cabin>>() {
            @Override
            public void onSuccess(List<Cabin> cabins) {
                roomTypeService.attachRoomTypes(cabins, new SupabaseCallback<List<Cabin>>() {
                    @Override
                    public void onSuccess(List<Cabin> enrichedCabins) {
                        List<AiRecommendation> recommendations = buildRecommendations(query, enrichedCabins);
                        attachAvailability(query, recommendations, callback);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public AiSearchQuery parseQuery(String message) {
        String original = message == null ? "" : message.trim();
        String normalized = normalize(original);
        LocalDate today = LocalDate.now();
        AiSearchQuery query = new AiSearchQuery();
        query.setOriginalMessage(original);
        query.setDestination(parseDestination(normalized));
        query.setAdults(Math.max(1, parseNumberBeforeUnit(normalized, "(nguoi lon|nguoi|khach|adult|adults)", 2)));
        query.setChildren(Math.max(0, parseNumberBeforeUnit(normalized, "(be|tre em|em be|child|children|kid|kids)", 0)));
        query.setRooms(Math.max(1, parseNumberBeforeUnit(normalized, "(phong|room|rooms)", 1)));
        query.setRequestedBeds(Math.max(0, parseNumberBeforeUnit(normalized, "(giuong|bed|beds)", 0)));
        query.setMaxPricePerNight(parseMaxPrice(normalized));
        query.setRoomQuery(parseRoomQuery(normalized));
        query.setAmenities(parseAmenities(normalized));
        parseDates(normalized, query, today);
        return query;
    }

    private List<AiRecommendation> buildRecommendations(AiSearchQuery query, List<Cabin> cabins) {
        List<AiRecommendation> recommendations = new ArrayList<>();
        for (Cabin cabin : cabins) {
            if (!matchesDestination(query, cabin) || !matchesAmenities(query, cabin)) {
                continue;
            }
            RoomType roomType = roomTypeService.findBestRoomType(
                    cabin,
                    query.getAdults(),
                    query.getChildren(),
                    query.getRequestedBeds(),
                    query.getRoomQuery()
            );
            if (roomType == null) {
                if (cabin.getMaxCapacity() > 0 && cabin.getMaxCapacity() < query.getTotalGuests()) {
                    continue;
                }
                if (!query.getRoomQuery().isEmpty() || query.getRequestedBeds() > 0) {
                    continue;
                }
            }
            double nightlyPrice = roomType == null ? cabin.displayPrice() : roomType.getBasePrice();
            if (query.getMaxPricePerNight() > 0 && nightlyPrice > query.getMaxPricePerNight()) {
                continue;
            }
            cabin.setMatchedRoomType(roomType);
            recommendations.add(new AiRecommendation(cabin, roomType, score(query, cabin, roomType), reasons(query, cabin, roomType)));
        }
        recommendations.sort(Comparator.comparingDouble(AiRecommendation::getScore).reversed());
        if (recommendations.size() > MAX_RESULTS) {
            return new ArrayList<>(recommendations.subList(0, MAX_RESULTS));
        }
        return recommendations;
    }

    private void attachAvailability(
            AiSearchQuery query,
            List<AiRecommendation> recommendations,
            SupabaseCallback<AiSearchResult> callback
    ) {
        if (recommendations.isEmpty() || !query.hasDateRange()) {
            callback.onSuccess(new AiSearchResult(query, recommendations));
            return;
        }

        AtomicInteger remaining = new AtomicInteger(recommendations.size());
        for (AiRecommendation recommendation : recommendations) {
            SupabaseCallback<BookingService.AvailabilityStatus> availabilityCallback =
                    new SupabaseCallback<BookingService.AvailabilityStatus>() {
                        @Override
                        public void onSuccess(BookingService.AvailabilityStatus status) {
                            recommendation.setAvailability(status.isAvailable(), status.getMessage());
                            finishAvailabilityCheck(query, recommendations, remaining, callback);
                        }

                        @Override
                        public void onError(String message) {
                            recommendation.setAvailabilityUnknown(message);
                            finishAvailabilityCheck(query, recommendations, remaining, callback);
                        }
                    };
            if (recommendation.getRoomType() == null) {
                bookingService.getCabinAvailability(
                        recommendation.getCabin().getId(),
                        query.getCheckIn(),
                        query.getCheckOut(),
                        availabilityCallback
                );
                continue;
            }
            bookingService.getRoomTypeAvailability(
                    recommendation.getCabin().getId(),
                    recommendation.getRoomType().getId(),
                    query.getCheckIn(),
                    query.getCheckOut(),
                    query.getRooms(),
                    availabilityCallback
            );
        }
    }

    private void finishAvailabilityCheck(
            AiSearchQuery query,
            List<AiRecommendation> recommendations,
            AtomicInteger remaining,
            SupabaseCallback<AiSearchResult> callback
    ) {
        if (remaining.decrementAndGet() != 0) {
            return;
        }
        recommendations.sort(
                Comparator.comparingInt(AiRecommendation::availabilityRank).reversed()
                        .thenComparing(Comparator.comparingDouble(AiRecommendation::getScore).reversed())
        );
        callback.onSuccess(new AiSearchResult(query, recommendations));
    }

    private boolean matchesDestination(AiSearchQuery query, Cabin cabin) {
        if (query.getDestination().isEmpty()) {
            return true;
        }
        String haystack = normalize(cabin.getName() + " " + cabin.getLocation() + " " + cabin.getAddress() + " " + cabin.getDistrict());
        return haystack.contains(normalize(query.getDestination()));
    }

    private boolean matchesAmenities(AiSearchQuery query, Cabin cabin) {
        if (query.getAmenities().isEmpty()) {
            return true;
        }
        String haystack = normalize(cabin.getAmenities() + " " + cabin.getDescription() + " " + cabin.getLocation());
        for (String amenity : query.getAmenities()) {
            if (!haystack.contains(normalize(amenity))) {
                return false;
            }
        }
        return true;
    }

    private double score(AiSearchQuery query, Cabin cabin, RoomType roomType) {
        double score = cabin.getReviewScore() * 10;
        score += cabin.getStarRating() * 2;
        score += query.getDestination().isEmpty() ? 0 : 12;
        score += query.getAmenities().size() * 5;
        double nightlyPrice = roomType == null ? cabin.displayPrice() : roomType.getBasePrice();
        score += Math.max(0, 220 - nightlyPrice) * 0.04;
        if (roomType != null && roomType.effectiveMaxAdults() == query.getAdults()) {
            score += 4;
        }
        if (roomType != null && roomType.effectiveSleepingCapacity() >= query.getTotalGuests()) {
            score += 5;
        }
        if (roomType != null && !query.getRoomQuery().isEmpty()
                && normalize(roomType.getCategory()).contains(normalize(query.getRoomQuery()))) {
            score += 6;
        }
        return score;
    }

    private List<String> reasons(AiSearchQuery query, Cabin cabin, RoomType roomType) {
        List<String> reasons = new ArrayList<>();
        if (!query.getDestination().isEmpty()) {
            reasons.add("Đúng khu vực " + query.getDestination());
        }
        if (roomType != null && roomType.effectiveMaxAdults() >= query.getAdults()
                && roomType.effectiveSleepingCapacity() >= query.getTotalGuests()) {
            reasons.add("Phù hợp " + query.occupancyLabel());
        } else if (roomType == null && cabin.getMaxCapacity() >= query.getTotalGuests()) {
            reasons.add("Cabin phù hợp " + query.getTotalGuests() + " khách");
        }
        if (roomType != null && query.getRequestedBeds() > 0
                && roomType.effectiveBedCount() >= query.getRequestedBeds()) {
            reasons.add("Đủ " + query.getRequestedBeds() + " giường");
        }
        if (!query.getAmenities().isEmpty()) {
            reasons.add("Có " + join(query.getAmenities(), ", "));
        }
        if (cabin.getReviewScore() > 0) {
            reasons.add("Đánh giá " + String.format(Locale.US, "%.1f", cabin.getReviewScore()));
        }
        return reasons;
    }

    private String parseDestination(String normalized) {
        if (containsAny(normalized, "ho chi minh", "sai gon", "saigon", "tphcm", "tp ho chi minh")) {
            return "Ho Chi Minh City";
        }
        if (containsAny(normalized, "vung tau")) {
            return "Vung Tau";
        }
        if (containsAny(normalized, "ha noi", "hanoi")) {
            return "Hanoi";
        }
        if (containsAny(normalized, "da nang", "danang")) {
            return "Da Nang";
        }
        if (containsAny(normalized, "da lat", "dalat")) {
            return "Da Lat";
        }
        return "";
    }

    private List<String> parseAmenities(String normalized) {
        List<String> amenities = new ArrayList<>();
        addAmenityIfFound(amenities, normalized, "WiFi", "wifi", "internet");
        addAmenityIfFound(amenities, normalized, "Breakfast", "bua sang", "an sang", "breakfast");
        addAmenityIfFound(amenities, normalized, "Parking", "do xe", "bai xe", "parking");
        addAmenityIfFound(amenities, normalized, "Pool", "ho boi", "be boi", "pool");
        addAmenityIfFound(amenities, normalized, "Sea view", "huong bien", "view bien", "gan bien", "bien", "sea view");
        addAmenityIfFound(amenities, normalized, "Balcony", "ban cong", "balcony");
        addAmenityIfFound(amenities, normalized, "Air conditioning", "dieu hoa", "may lanh", "air conditioning");
        addAmenityIfFound(amenities, normalized, "Private bathroom", "phong tam rieng", "private bathroom");
        return amenities;
    }

    private void addAmenityIfFound(List<String> amenities, String normalized, String label, String... keywords) {
        if (amenities.contains(label)) {
            return;
        }
        for (String keyword : keywords) {
            if (normalized.contains(keyword)) {
                amenities.add(label);
                return;
            }
        }
    }

    private String parseRoomQuery(String normalized) {
        if (containsAny(normalized, "suite", "can ho", "phong khach")) {
            return "Suite";
        }
        if (containsAny(normalized, "deluxe", "sang hon", "cao cap vua")) {
            return "Deluxe";
        }
        if (containsAny(normalized, "superior", "cao cap")) {
            return "Superior";
        }
        if (containsAny(normalized, "standard", "tieu chuan", "re", "tiet kiem")) {
            return "Standard";
        }
        if (containsAny(normalized, "gia dinh", "family")) {
            return "Deluxe";
        }
        return "";
    }

    private void parseDates(String normalized, AiSearchQuery query, LocalDate today) {
        LocalDate checkIn = null;
        LocalDate checkOut = null;

        Matcher sameMonthRange = Pattern.compile("(\\d{1,2})\\s*-\\s*(\\d{1,2})\\s*/\\s*(\\d{1,2})").matcher(normalized);
        if (sameMonthRange.find()) {
            int startDay = parseInt(sameMonthRange.group(1), today.getDayOfMonth());
            int endDay = parseInt(sameMonthRange.group(2), today.plusDays(1).getDayOfMonth());
            int month = parseInt(sameMonthRange.group(3), today.getMonthValue());
            checkIn = safeDate(today.getYear(), month, startDay, null);
            checkOut = safeDate(today.getYear(), month, endDay, null);
        } else {
            Matcher fullRange = Pattern.compile("(\\d{1,2})\\s*/\\s*(\\d{1,2})\\s*(?:-|den|toi|to)\\s*(\\d{1,2})\\s*/\\s*(\\d{1,2})").matcher(normalized);
            if (fullRange.find()) {
                checkIn = safeDate(today.getYear(), parseInt(fullRange.group(2), today.getMonthValue()), parseInt(fullRange.group(1), today.getDayOfMonth()), null);
                checkOut = safeDate(today.getYear(), parseInt(fullRange.group(4), today.getMonthValue()), parseInt(fullRange.group(3), today.plusDays(1).getDayOfMonth()), null);
            }
        }

        if (checkIn != null && checkOut != null && checkOut.isAfter(checkIn)) {
            query.setCheckIn(checkIn.toString());
            query.setCheckOut(checkOut.toString());
        }
    }

    private int parseNumberBeforeUnit(String normalized, String unitRegex, int fallback) {
        Matcher matcher = Pattern.compile("(\\d+)\\s*" + unitRegex).matcher(normalized);
        if (matcher.find()) {
            return parseInt(matcher.group(1), fallback);
        }
        return fallback;
    }

    private double parseMaxPrice(String normalized) {
        if (!containsAny(normalized, "gia", "duoi", "toi da", "ngan sach", "budget", "usd", "$", "trieu", "vnd", "do la")) {
            return 0;
        }
        Matcher dollarFirst = Pattern.compile("\\$\\s*(\\d+(?:[\\.,]\\d+)?)").matcher(normalized);
        if (dollarFirst.find()) {
            return parseDouble(dollarFirst.group(1), 0);
        }

        Matcher explicitUnit = Pattern.compile("(\\d+(?:[\\.,]\\d+)?)\\s*(trieu|tr|usd|do|\\$|vnd)").matcher(normalized);
        while (explicitUnit.find()) {
            double value = parseDouble(explicitUnit.group(1), 0);
            String unit = explicitUnit.group(2) == null ? "" : explicitUnit.group(2);
            if (value <= 0) {
                continue;
            }
            if (unit.equals("trieu") || unit.equals("tr") || unit.equals("vnd")) {
                return value * 1_000_000d / 25_000d;
            }
            return value;
        }

        Matcher keywordPrice = Pattern.compile("(?:gia|duoi|toi da|ngan sach|budget|tam)\\s*(\\d+(?:[\\.,]\\d+)?)").matcher(normalized);
        if (keywordPrice.find()) {
            return parseDouble(keywordPrice.group(1), 0);
        }
        return 0;
    }

    private LocalDate safeDate(int year, int month, int day, LocalDate fallback) {
        try {
            return LocalDate.of(year, month, day);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        String safe = value == null ? "" : value.toLowerCase(Locale.US);
        String withoutDiacritics = Normalizer.normalize(safe, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutDiacritics.replace('đ', 'd').replace('Đ', 'd').trim();
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String join(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(separator);
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    public static class AiSearchQuery {
        private String originalMessage = "";
        private String destination = "";
        private String checkIn = "";
        private String checkOut = "";
        private int adults = 2;
        private int children = 0;
        private int rooms = 1;
        private int requestedBeds = 0;
        private double maxPricePerNight = 0;
        private String roomQuery = "";
        private List<String> amenities = new ArrayList<>();

        public AiSearchQuery copy() {
            AiSearchQuery copy = new AiSearchQuery();
            copy.originalMessage = originalMessage;
            copy.destination = destination;
            copy.checkIn = checkIn;
            copy.checkOut = checkOut;
            copy.adults = adults;
            copy.children = children;
            copy.rooms = rooms;
            copy.requestedBeds = requestedBeds;
            copy.maxPricePerNight = maxPricePerNight;
            copy.roomQuery = roomQuery;
            copy.amenities = new ArrayList<>(amenities);
            return copy;
        }

        public String getOriginalMessage() {
            return originalMessage;
        }

        public void setOriginalMessage(String originalMessage) {
            this.originalMessage = originalMessage == null ? "" : originalMessage;
        }

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination == null ? "" : destination;
        }

        public String getCheckIn() {
            return checkIn;
        }

        public void setCheckIn(String checkIn) {
            this.checkIn = checkIn == null ? "" : checkIn;
        }

        public String getCheckOut() {
            return checkOut;
        }

        public void setCheckOut(String checkOut) {
            this.checkOut = checkOut == null ? "" : checkOut;
        }

        public int getAdults() {
            return adults;
        }

        public void setAdults(int adults) {
            this.adults = adults;
        }

        public int getChildren() {
            return children;
        }

        public void setChildren(int children) {
            this.children = Math.max(0, children);
        }

        public int getTotalGuests() {
            return Math.max(1, adults) + Math.max(0, children);
        }

        public String occupancyLabel() {
            if (children <= 0) {
                return adults + " ngÆ°á»i lá»›n";
            }
            return adults + " ngÆ°á»i lá»›n + " + children + " tráº» em";
        }

        public int getRooms() {
            return rooms;
        }

        public void setRooms(int rooms) {
            this.rooms = rooms;
        }

        public int getRequestedBeds() {
            return requestedBeds;
        }

        public void setRequestedBeds(int requestedBeds) {
            this.requestedBeds = requestedBeds;
        }

        public double getMaxPricePerNight() {
            return maxPricePerNight;
        }

        public void setMaxPricePerNight(double maxPricePerNight) {
            this.maxPricePerNight = maxPricePerNight;
        }

        public String getRoomQuery() {
            return roomQuery;
        }

        public void setRoomQuery(String roomQuery) {
            this.roomQuery = roomQuery == null ? "" : roomQuery;
        }

        public List<String> getAmenities() {
            return amenities;
        }

        public void setAmenities(List<String> amenities) {
            this.amenities = amenities == null ? new ArrayList<>() : amenities;
        }

        public boolean hasDateRange() {
            try {
                return LocalDate.parse(checkOut).isAfter(LocalDate.parse(checkIn));
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    public static class AiRecommendation {
        private final Cabin cabin;
        private final RoomType roomType;
        private final double score;
        private final List<String> reasons;
        private boolean availabilityChecked;
        private boolean available;
        private boolean availabilityUnknown;
        private String availabilityMessage = "";

        public AiRecommendation(Cabin cabin, RoomType roomType, double score, List<String> reasons) {
            this.cabin = cabin;
            this.roomType = roomType;
            this.score = score;
            this.reasons = reasons;
        }

        public Cabin getCabin() {
            return cabin;
        }

        public RoomType getRoomType() {
            return roomType;
        }

        public double getScore() {
            return score;
        }

        public List<String> getReasons() {
            return reasons;
        }

        private void setAvailability(boolean available, String message) {
            this.availabilityChecked = true;
            this.available = available;
            this.availabilityUnknown = false;
            this.availabilityMessage = message == null ? "" : message;
        }

        private void setAvailabilityUnknown(String message) {
            this.availabilityChecked = true;
            this.available = false;
            this.availabilityUnknown = true;
            this.availabilityMessage = message == null ? "" : message;
        }

        private int availabilityRank() {
            if (!availabilityChecked) {
                return 1;
            }
            if (availabilityUnknown) {
                return 0;
            }
            return available ? 2 : -1;
        }

        public boolean isAvailabilityChecked() {
            return availabilityChecked;
        }

        public boolean isAvailable() {
            return available;
        }

        public boolean isAvailabilityUnknown() {
            return availabilityUnknown;
        }

        public String getAvailabilityMessage() {
            return availabilityMessage;
        }

        public int getNights(AiSearchQuery query) {
            if (query == null || !query.hasDateRange()) {
                return 0;
            }
            return (int) ChronoUnit.DAYS.between(
                    LocalDate.parse(query.getCheckIn()),
                    LocalDate.parse(query.getCheckOut())
            );
        }

        public double getEstimatedTotal(AiSearchQuery query) {
            double nightlyPrice = roomType == null ? cabin.displayPrice() : roomType.getBasePrice();
            int units = roomType == null ? 1 : Math.max(1, query.getRooms());
            return nightlyPrice * units * getNights(query);
        }
    }

    public static class AiSearchResult {
        private final AiSearchQuery query;
        private final List<AiRecommendation> recommendations;

        public AiSearchResult(AiSearchQuery query, List<AiRecommendation> recommendations) {
            this.query = query;
            this.recommendations = recommendations;
        }

        public AiSearchQuery getQuery() {
            return query;
        }

        public List<AiRecommendation> getRecommendations() {
            return recommendations;
        }
    }
}
