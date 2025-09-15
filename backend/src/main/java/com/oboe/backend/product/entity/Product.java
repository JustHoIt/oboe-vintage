package com.oboe.backend.product.entity;


import com.oboe.backend.common.domain.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Product extends BaseTimeEntity {

  // 고유 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String name; // 상품명

  @Column(nullable = false)
  private String description; // 상품 설명

  @ManyToMany
  @JoinTable(name = "product_categories",
      joinColumns = @JoinColumn(name = "product_id"),
      inverseJoinColumns = @JoinColumn(name = "category_id"))
  @Builder.Default
  private Set<ProductCategory> categories = new HashSet<>(); // 상품 카테고리

  private String sku; // 상품 코드 (SKU)

  @Column(nullable = false)
  private BigDecimal price; // 상품 가격

  @Column(nullable = false)
  private Integer stockQuantity; // 재고 수량

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ProductImage> productImages = new ArrayList<>(); // 상품 이미지 목록

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ProductStatus productStatus; // 상품 판매 상태

  private String brand; // 브랜드명

  private String yearOfRelease; // 출시 년도

  private String size; // 사이즈(신발, 옷)

  private String texture; // 소재 및 재질

  @Enumerated(EnumType.STRING)
  private Condition condition; // 상품 상태 (새상품, 중고 등)

  private Integer views; // 조회수

  /**
   * 조회수 1 증가
   */
  public void increaseViews() {
    this.views++;
  }

  /**
   * 상품 정보 업데이트
   */
  public void updateProductInfo(String name, String description, String sku, 
                               BigDecimal price, Integer stockQuantity, ProductStatus productStatus,
                               String brand, String yearOfRelease, String size, String texture, 
                               Condition condition) {
    if (name != null && !name.trim().isEmpty()) {
      this.name = name;
    }
    if (description != null) {
      this.description = description;
    }
    if (sku != null) {
      this.sku = sku;
    }
    if (price != null) {
      this.price = price;
    }
    if (stockQuantity != null) {
      this.stockQuantity = stockQuantity;
    }
    if (productStatus != null) {
      this.productStatus = productStatus;
    }
    if (brand != null) {
      this.brand = brand;
    }
    if (yearOfRelease != null) {
      this.yearOfRelease = yearOfRelease;
    }
    if (size != null) {
      this.size = size;
    }
    if (texture != null) {
      this.texture = texture;
    }
    if (condition != null) {
      this.condition = condition;
    }
  }

  /**
   * 카테고리 업데이트
   */
  public void updateCategories(Set<ProductCategory> categories) {
    this.categories.clear();
    if (categories != null) {
      this.categories.addAll(categories);
    }
  }

  /**
   * 상품 상태 변경
   */
  public void changeStatus(ProductStatus status) {
    this.productStatus = status;
  }
}
