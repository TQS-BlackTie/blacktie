package tqs.blacktie.dto;

public class ReviewRequest {
    private Long bookingId;
    private Integer rating;
    private String comment;

    public ReviewRequest() {}

    public ReviewRequest(Long bookingId, Integer rating, String comment) {
        this.bookingId = bookingId;
        this.rating = rating;
        this.comment = comment;
    }

    public Long getBookingId() { return bookingId; }
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }

    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
}
