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

  //고유 식별자
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String name; // 이름

  @Column(nullable = false)
  private String description; // 설명

  @ManyToMany
  @JoinTable(name = "product_categories",
      joinColumns = @JoinColumn(name = "product_id"),
      inverseJoinColumns = @JoinColumn(name = "category_id"))
  @Builder.Default
  private Set<ProductCategory> categories = new HashSet<>(); // 카테고리

  @Column
  private String sku; //상품 코드(존재 할 경우에만)

  @Column(nullable = false)
  private BigDecimal price; // 가격

  @Column(nullable = false)
  private Integer stockQuantity; // 수량

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ProductImage> productImages = new ArrayList<>(); //상품 이미지

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ProductStatus productStatus; // 상품 상태

  private String brand; // 브랜드명

  private String yearOfRelease; // 출시 년도

  private String size; // 사이즈(신발, 옷)

  private String texture; // 소재 및 재질

  @Enumerated(EnumType.STRING)
  private Condition condition; // 상태

  private Integer views; //조회수

  public void increaseViews() {
    this.views++;
  }


}
