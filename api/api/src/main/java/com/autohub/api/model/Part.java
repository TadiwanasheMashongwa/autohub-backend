package com.autohub.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parts")
public class Part {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Part name is required")
  private String name;

  @NotBlank(message = "SKU is required")
  @Column(unique = true, nullable = false)
  private String sku;

  @NotBlank(message = "Barcode is required")
  @Column(unique = true, nullable = false)
  private String barcode;

  private String oemNumber;

  @Column(columnDefinition = "TEXT")
  private String description;

  @NotNull(message = "Price is required")
  @DecimalMin(value = "0.0", inclusive = false)
  private BigDecimal price;

  @NotNull(message = "Stock quantity is required")
  private Integer stockQuantity;

  private String brand;
  private String condition;
  private String imageUrl;

  @ManyToOne
  @JoinColumn(name = "category_id")
  private Category category;

  @OneToMany(mappedBy = "part", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Review> reviews = new ArrayList<>();

  private Double averageRating = 0.0;

  // NEW: OPTIMISTIC LOCKING VERSION
  @Version
  private Long version;

  public Part() {}

  // --- MANUAL GETTERS & SETTERS ---
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }
  public String getBarcode() { return barcode; }
  public void setBarcode(String barcode) { this.barcode = barcode; }
  public String getOemNumber() { return oemNumber; }
  public void setOemNumber(String oemNumber) { this.oemNumber = oemNumber; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public BigDecimal getPrice() { return price; }
  public void setPrice(BigDecimal price) { this.price = price; }
  public Integer getStockQuantity() { return stockQuantity; }
  public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
  public String getBrand() { return brand; }
  public void setBrand(String brand) { this.brand = brand; }
  public String getCondition() { return condition; }
  public void setCondition(String condition) { this.condition = condition; }
  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
  public Category getCategory() { return category; }
  public void setCategory(Category category) { this.category = category; }
  public List<Review> getReviews() { return reviews; }
  public void setReviews(List<Review> reviews) { this.reviews = reviews; }
  public Double getAverageRating() { return averageRating; }
  public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

  // NEW VERSION GETTER
  public Long getVersion() { return version; }
}