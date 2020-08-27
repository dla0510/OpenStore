package com.example.openstore;

public class MyGooglePlaces {
    private String id;
    private String name;
    private String category;
    private String rating;
    private String opennow;
    private String vicinity;
    private String photo_ref;
    private String phone_number;
    private String weekday;
    private double latitude, longitude;


    public MyGooglePlaces() {
        this.id = "";
        this.name = "";
        this.category = "";
        this.rating = "";
        this.opennow = "";
        this.vicinity = "";
        this.phone_number="";
        this.weekday="";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getWeekday() {
        return weekday;
    }

    public void setWeekday(String weekday) {
        this.weekday = weekday;
    }

    public String getPhoto_ref() {
        return photo_ref;
    }

    public void setPhoto_ref(String photo_ref) {
        this.photo_ref = photo_ref;
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

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getOpen() {
        return opennow;
    }

    public void setOpenNow(String open) {
        this.opennow = open;
        //if open_now is true, opennow = 'YES' , else opennow='NO'
        //if there is no open_now data, opennow = 'Not Known'
    }

    public String getVicinity() {
        return vicinity;
    }

    public void setVicinity(String vicinity) {
        this.vicinity = vicinity;
    }

    public Double getLat() {
        return latitude;
    }

    public Double getLng() {
        return longitude;
    }

    public void setLatLng(double lat, double lng) {
        this.latitude = lat;
        this.longitude = lng;
    }
}