package com.autohub.api.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

  @Column(unique = true, nullable = false)
  private String sku;

  @Column(unique = true, nullable = false)
  private String barcode;

  private String oemNumber;

  @Column(columnDefinition = "TEXT")
  private String description;

  @NotNull(message = "Price is required")
  private BigDecimal price;

  @NotNull(message = "Stock quantity is required")
  private Integer stockQuantity;

  private String brand;
  private String condition;
  private String imageUrl;

  @ManyToOne
  @JoinColumn(name = "category_id")
  private Category category;

  // Use orphanRemoval to manage life cycle strictly
  @OneToMany(mappedBy = "part", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @JsonManagedReference
  private List<Review> reviews = new ArrayList<>();

  @ManyToMany
  @JoinTable(
          name = "part_vehicle_compatibility",
          joinColumns = @JoinColumn(name = "part_id"),
          inverseJoinColumns = @JoinColumn(name = "vehicle_id")
  )
  @JsonIgnore
  private List<Vehicle> compatibleVehicles = new ArrayList<>();

  private Double averageRating = 0.0;

  @Version
  private Long version;

  public Part() {}

  // Helper to maintain bidirectional sync
  public void addReview(Review review) {
    reviews.add(review);
    review.setPart(this);
  }

  // --- GETTERS & SETTERS (Keep your existing ones) ---
  public Long getId() { return id; }
  public String getName() { return name; }
  public String getSku() { return sku; }
  public String getBarcode() { return barcode; }
  public BigDecimal getPrice() { return price; }
  public Integer getStockQuantity() { return stockQuantity; }
  public List<Review> getReviews() { return reviews; }
  public Double getAverageRating() { return averageRating; }

  public void setId(Long id) { this.id = id; }
  public void setName(String name) { this.name = name; }
  public void setSku(String sku) { this.sku = sku; }
  public void setBarcode(String barcode) { this.barcode = barcode; }
  public void setPrice(BigDecimal price) { this.price = price; }
  public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
  public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
  public void setVersion(Long version) { this.version = version; }
}