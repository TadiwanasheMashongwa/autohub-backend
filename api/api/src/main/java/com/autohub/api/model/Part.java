package com.autohub.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "parts")
@NoArgsConstructor
@AllArgsConstructor
public class Part {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Part name is required")
  @Column(nullable = false)
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
  @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
  private BigDecimal price;

  @NotNull(message = "Stock quantity is required")
  private Integer stockQuantity;

  @NotBlank(message = "Brand is required")
  private String brand;

  private String condition;

  // NEW: Asset field for frontend images
  private String imageUrl;

  @ManyToOne
  @JoinColumn(name = "category_id")
  private Category category;

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

  // NEW GETTER/SETTER
  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

  public Category getCategory() { return category; }
  public void setCategory(Category category) { this.category = category; }
}