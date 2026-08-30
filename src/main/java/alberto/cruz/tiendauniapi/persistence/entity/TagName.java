package alberto.cruz.tiendauniapi.persistence.entity;

// Tags behave like social-media hashtags for publication search filters.
// Distinct from CategoryName (which classifies the product itself): a product
// belongs to one category and a publication can carry many tags.
// Extensible: add new constants here when introducing new filter dimensions.
public enum TagName {
    NEW,
    LIKE_NEW,
    USED,
    HANDMADE,
    CASH_ACCEPTED,
    TRANSFER_ACCEPTED,
    CARD_ACCEPTED,
    NEGOTIABLE,
    FIXED_PRICE,
    BULK_PRICE,
    FREE_DELIVERY,
    PICKUP_ONLY,
    URGENT
}
