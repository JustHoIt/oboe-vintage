package com.oboe.backend.product.repository;

import static com.oboe.backend.product.entity.QProduct.product;
import static com.oboe.backend.product.entity.QProductCategory.productCategory;

import com.oboe.backend.product.dto.request.ProductSearchRequest;
import com.oboe.backend.product.entity.Product;
import com.oboe.backend.product.entity.ProductStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public Page<Product> searchProducts(ProductSearchRequest searchRequest, Pageable pageable) {
    BooleanBuilder builder = new BooleanBuilder();

    // 기본 조건: INACTIVE 상태 제외
    builder.and(product.productStatus.ne(ProductStatus.INACTIVE));

    // 키워드 검색 (상품명, 설명, 브랜드)
    if (StringUtils.hasText(searchRequest.getKeyword())) {
      BooleanBuilder keywordBuilder = new BooleanBuilder();
      keywordBuilder.or(product.name.containsIgnoreCase(searchRequest.getKeyword()))
          .or(product.description.containsIgnoreCase(searchRequest.getKeyword()))
          .or(product.brand.containsIgnoreCase(searchRequest.getKeyword()));
      builder.and(keywordBuilder);
    }

    // 상품 상태 필터
    if (searchRequest.getStatus() != null) {
      builder.and(product.productStatus.eq(searchRequest.getStatus()));
    }

    // 카테고리 필터
    if (searchRequest.getCategoryId() != null) {
      builder.and(product.categories.any().id.eq(searchRequest.getCategoryId()));
    }

    // 브랜드 필터
    if (StringUtils.hasText(searchRequest.getBrand())) {
      builder.and(product.brand.eq(searchRequest.getBrand()));
    }

    // 컨디션 필터
    if (StringUtils.hasText(searchRequest.getCondition())) {
      builder.and(product.condition.stringValue().eq(searchRequest.getCondition()));
    }

    // 기본 쿼리 생성
    JPAQuery<Product> query = queryFactory
        .selectFrom(product)
        .leftJoin(product.categories, productCategory).fetchJoin()
        .where(builder)
        .distinct();

    // 정렬 조건 적용
    OrderSpecifier<?> orderSpecifier = getOrderSpecifier(searchRequest.getSortBy());
    if (orderSpecifier != null) {
      query.orderBy(orderSpecifier);
    }

    // 페이징 적용
    List<Product> products = query
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    // 총 개수 조회
    long total = queryFactory
        .selectFrom(product)
        .where(builder)
        .fetchCount();

    return new PageImpl<>(products, pageable, total);
  }

  @Override
  public Page<Product> searchByKeyword(String keyword, Pageable pageable) {
    BooleanBuilder builder = new BooleanBuilder();

    // 기본 조건: INACTIVE 상태 제외
    builder.and(product.productStatus.ne(ProductStatus.INACTIVE));

    // 키워드 검색
    if (StringUtils.hasText(keyword)) {
      BooleanBuilder keywordBuilder = new BooleanBuilder();
      keywordBuilder.or(product.name.containsIgnoreCase(keyword))
          .or(product.description.containsIgnoreCase(keyword))
          .or(product.brand.containsIgnoreCase(keyword));
      builder.and(keywordBuilder);
    }

    JPAQuery<Product> query = queryFactory
        .selectFrom(product)
        .where(builder)
        .orderBy(product.createdAt.desc()); // 기본 정렬: 최신순

    List<Product> products = query
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    long total = queryFactory
        .selectFrom(product)
        .where(builder)
        .fetchCount();

    return new PageImpl<>(products, pageable, total);
  }

  @Override
  public Page<Product> searchByFilters(ProductSearchRequest searchRequest, Pageable pageable) {
    BooleanBuilder builder = new BooleanBuilder();

    // 기본 조건: INACTIVE 상태 제외
    builder.and(product.productStatus.ne(ProductStatus.INACTIVE));

    // 상품 상태 필터
    if (searchRequest.getStatus() != null) {
      builder.and(product.productStatus.eq(searchRequest.getStatus()));
    }

    // 카테고리 필터
    if (searchRequest.getCategoryId() != null) {
      builder.and(product.categories.any().id.eq(searchRequest.getCategoryId()));
    }

    // 브랜드 필터
    if (StringUtils.hasText(searchRequest.getBrand())) {
      builder.and(product.brand.eq(searchRequest.getBrand()));
    }

    // 컨디션 필터
    if (StringUtils.hasText(searchRequest.getCondition())) {
      builder.and(product.condition.stringValue().eq(searchRequest.getCondition()));
    }

    JPAQuery<Product> query = queryFactory
        .selectFrom(product)
        .where(builder);

    // 정렬 조건 적용
    OrderSpecifier<?> orderSpecifier = getOrderSpecifier(searchRequest.getSortBy());
    if (orderSpecifier != null) {
      query.orderBy(orderSpecifier);
    } else {
      query.orderBy(product.createdAt.desc()); // 기본 정렬: 최신순
    }

    List<Product> products = query
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    long total = queryFactory
        .selectFrom(product)
        .where(builder)
        .fetchCount();

    return new PageImpl<>(products, pageable, total);
  }

  /**
   * 정렬 조건에 따른 OrderSpecifier 반환
   */
  private OrderSpecifier<?> getOrderSpecifier(String sortBy) {
    if (!StringUtils.hasText(sortBy)) {
      return product.createdAt.desc(); // 기본값: 최신순
    }

    return switch (sortBy.toLowerCase()) {
      case "latest" -> product.createdAt.desc();
      case "oldest" -> product.createdAt.asc();
      case "views" -> product.views.desc();
      case "price_asc" -> product.price.asc();
      case "price_desc" -> product.price.desc();
      default -> product.createdAt.desc();
    };
  }
}
