package com.example.hotel_booking_app.data.models;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class RoomType {
    @SerializedName("_id")
    private String id;

    private String cabinId;
    private String name;
    private String category;
    private String description;
    private int maxGuests;
    private int maxAdults;
    private int totalRooms;
    private double basePrice;
    private String beds;
    private String bedType;
    private int bedCount;
    private int sleepingCapacity;
    private String bedSummary;
    private JsonElement bedConfig;
    private double bedWidthM;
    private double bedLengthM;
    private String size;
    private int sizeM2;
    private boolean hasLivingRoom;
    private String amenities;
    private String image;
    private boolean isActive = true;
    private String createdAt;
    private String updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCabinId() {
        return cabinId;
    }

    public void setCabinId(String cabinId) {
        this.cabinId = cabinId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(int maxGuests) {
        this.maxGuests = maxGuests;
    }

    public int getMaxAdults() {
        return maxAdults;
    }

    public void setMaxAdults(int maxAdults) {
        this.maxAdults = maxAdults;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public String getBeds() {
        return beds;
    }

    public void setBeds(String beds) {
        this.beds = beds;
    }

    public String getBedType() {
        return bedType;
    }

    public void setBedType(String bedType) {
        this.bedType = bedType;
    }

    public int getBedCount() {
        return bedCount;
    }

    public void setBedCount(int bedCount) {
        this.bedCount = bedCount;
    }

    public int getSleepingCapacity() {
        return sleepingCapacity;
    }

    public void setSleepingCapacity(int sleepingCapacity) {
        this.sleepingCapacity = sleepingCapacity;
    }

    public String getBedSummary() {
        return bedSummary;
    }

    public void setBedSummary(String bedSummary) {
        this.bedSummary = bedSummary;
    }

    public JsonElement getBedConfig() {
        return bedConfig;
    }

    public void setBedConfig(JsonElement bedConfig) {
        this.bedConfig = bedConfig;
    }

    public double getBedWidthM() {
        return bedWidthM;
    }

    public void setBedWidthM(double bedWidthM) {
        this.bedWidthM = bedWidthM;
    }

    public double getBedLengthM() {
        return bedLengthM;
    }

    public void setBedLengthM(double bedLengthM) {
        this.bedLengthM = bedLengthM;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getSizeM2() {
        return sizeM2;
    }

    public void setSizeM2(int sizeM2) {
        this.sizeM2 = sizeM2;
    }

    public boolean hasLivingRoom() {
        return hasLivingRoom;
    }

    public void setHasLivingRoom(boolean hasLivingRoom) {
        this.hasLivingRoom = hasLivingRoom;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String displayName() {
        return titleLabel();
    }

    public String titleLabel() {
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        if (category != null && !category.trim().isEmpty()) {
            return translateCategory(category.trim());
        }
        return "Phòng";
    }

    public String categoryLabel() {
        if (category == null || category.trim().isEmpty()) {
            return "Loại phòng";
        }
        return translateCategory(category.trim());
    }

    public String sizeLabel() {
        if (sizeM2 > 0) {
            return sizeM2 + " m2";
        }
        return size == null || size.trim().isEmpty() ? "Chưa có diện tích" : size.trim();
    }

    public String bedLabel() {
        String label = bedSummary != null && !bedSummary.trim().isEmpty() ? bedSummary.trim() : beds;
        if (label != null && !label.trim().isEmpty()) {
            return translateBedLabel(label.trim());
        }
        return "Chưa có thông tin giường";
    }

    public int effectiveMaxAdults() {
        return maxAdults > 0 ? maxAdults : maxGuests;
    }

    public int effectiveSleepingCapacity() {
        if (sleepingCapacity > 0) {
            return sleepingCapacity;
        }
        return Math.max(effectiveMaxAdults(), maxGuests);
    }

    public int effectiveBedCount() {
        return bedCount > 0 ? bedCount : inferBedCount();
    }

    public boolean fitsRoomSizeForGuests(int guests) {
        if (sizeM2 <= 0 || guests <= 0) {
            return true;
        }
        return sizeM2 >= minimumSizeForGuests(guests);
    }

    public int minimumSizeForGuests(int guests) {
        int normalizedGuests = Math.max(1, guests);
        if (normalizedGuests <= 1) {
            return 18;
        }
        if (normalizedGuests == 2) {
            return 22;
        }
        if (normalizedGuests == 3) {
            return 30;
        }
        if (normalizedGuests == 4) {
            return 40;
        }
        return 50 + (normalizedGuests - 5) * 6;
    }

    private String translateCategory(String value) {
        if ("standard".equalsIgnoreCase(value)) {
            return "Tiêu chuẩn";
        }
        if ("solo".equalsIgnoreCase(value)) {
            return "Phòng đơn";
        }
        if ("twin".equalsIgnoreCase(value)) {
            return "Phòng 2 giường đơn";
        }
        if ("superior".equalsIgnoreCase(value)) {
            return "Cao cấp";
        }
        if ("deluxe".equalsIgnoreCase(value)) {
            return "Deluxe";
        }
        if ("suite".equalsIgnoreCase(value)) {
            return "Suite";
        }
        if ("family".equalsIgnoreCase(value)) {
            return "Gia đình";
        }
        if ("room".equalsIgnoreCase(value)) {
            return "Phòng";
        }
        return value;
    }

    private String translateBedLabel(String value) {
        String normalized = value.trim();
        normalized = normalized.replaceAll("(?i)1\\s+single\\s+beds?", "1 giường đơn");
        normalized = normalized.replaceAll("(?i)2\\s+single\\s+beds?", "2 giường đơn");
        normalized = normalized.replaceAll("(?i)1\\s+double\\s+beds?", "1 giường đôi");
        normalized = normalized.replaceAll("(?i)2\\s+double\\s+beds?", "2 giường đôi");
        normalized = normalized.replaceAll("(?i)1\\s+queen\\s+beds?", "1 giường Queen");
        normalized = normalized.replaceAll("(?i)2\\s+queen\\s+beds?", "2 giường Queen");
        normalized = normalized.replaceAll("(?i)1\\s+king\\s+beds?", "1 giường King");
        normalized = normalized.replaceAll("(?i)2\\s+king\\s+beds?", "2 giường King");
        normalized = normalized.replaceAll("(?i)1\\s+sofa\\s+single", "1 sofa đơn");
        normalized = normalized.replaceAll("(?i)1\\s+sofa\\s+double", "1 sofa đôi");
        normalized = normalized.replaceAll("(?i)\\bsingle bed\\b", "giường đơn");
        normalized = normalized.replaceAll("(?i)\\bdouble bed\\b", "giường đôi");
        normalized = normalized.replaceAll("(?i)\\bqueen bed\\b", "giường Queen");
        normalized = normalized.replaceAll("(?i)\\bking bed\\b", "giường King");
        normalized = normalized.replaceAll("(?i)\\bsofa beds?\\b", "sofa");
        normalized = normalized.replace(" and ", " và ");
        normalized = normalized.replaceAll("(?i)\\bbeds\\b", "giường");
        normalized = normalized.replaceAll("(?i)\\bbed\\b", "giường");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }    private int inferBedCount() {
        String value = ((beds == null ? "" : beds) + " " + (bedSummary == null ? "" : bedSummary)).toLowerCase();
        int total = 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)\\s*(single|double|queen|king|sofa|extra|bunk|bed)").matcher(value);
        while (matcher.find()) {
            try {
                total += Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return total > 0 ? total : 1;
    }
}

