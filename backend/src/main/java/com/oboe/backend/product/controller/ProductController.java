package com.oboe.backend.product.controller;

import com.oboe.backend.common.dto.ResponseDto;
import com.oboe.backend.product.dto.request.ProductCreateRequest;
import com.oboe.backend.product.dto.request.ProductSearchRequest;
import com.oboe.backend.product.dto.request.ProductUpdateRequest;
import com.oboe.backend.product.dto.response.ProductListResponse;
import com.oboe.backend.product.dto.response.ProductResponse;
import com.oboe.backend.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "상품 관련 API")
@Slf4j
public class ProductController {

  private final ProductService productService;

  @PostMapping
  @Operation(summary = "상품 생성", description = "새로운 상품을 생성합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "상품 생성 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
      @ApiResponse(responseCode = "401", description = "인증 실패"),
      @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 권한 필요)"),
      @ApiResponse(responseCode = "409", description = "SKU 중복 등 비즈니스 규칙 위반"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  public ResponseEntity<ResponseDto<ProductResponse>> createProduct(
      @Valid @RequestBody ProductCreateRequest request,
      Authentication authentication) {

    ProductResponse response = productService.createProduct(request, authentication);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ResponseDto.success("상품이 성공적으로 생성되었습니다.", response));
  }

  @PutMapping("/{productId}")
  @Operation(summary = "상품 수정", description = "기존 상품 정보를 수정합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "상품 수정 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
      @ApiResponse(responseCode = "401", description = "인증 실패"),
      @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 권한 필요)"),
      @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
      @ApiResponse(responseCode = "409", description = "SKU 중복 등 비즈니스 규칙 위반"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  public ResponseEntity<ResponseDto<ProductResponse>> updateProduct(
      @Parameter(description = "상품 ID") @PathVariable Long productId,
      @Valid @RequestBody ProductUpdateRequest request,
      Authentication authentication) {

    ProductResponse response = productService.updateProduct(productId, request, authentication);

    return ResponseEntity.ok(ResponseDto.success("상품이 성공적으로 수정되었습니다.", response));
  }

  @DeleteMapping("/{productId}")
  @Operation(summary = "상품 삭제", description = "상품을 삭제합니다. (Soft Delete)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "상품 삭제 성공"),
      @ApiResponse(responseCode = "401", description = "인증 실패"),
      @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 권한 필요)"),
      @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
      @ApiResponse(responseCode = "400", description = "이미 삭제된 상품"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  public ResponseEntity<ResponseDto<Void>> deleteProduct(
      @Parameter(description = "상품 ID") @PathVariable Long productId,
      Authentication authentication) {

    productService.deleteProduct(productId, authentication);

    return ResponseEntity.ok(ResponseDto.success("상품이 성공적으로 삭제되었습니다.", null));
  }

  @GetMapping
  @Operation(summary = "상품 목록 조회", description = "상품 목록을 조회합니다. 검색어, 필터, 정렬 조건을 지원합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "상품 목록 조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  public ResponseEntity<ResponseDto<Page<ProductListResponse>>> getProducts(
      @Parameter(description = "검색어 (상품명, 브랜드, 설명)") @RequestParam(required = false) String keyword,
      @Parameter(description = "상품 상태 필터") @RequestParam(required = false) String status,
      @Parameter(description = "정렬 기준 (latest, oldest, views, price_asc, price_desc)") @RequestParam(required = false) String sortBy,
      @Parameter(description = "카테고리 ID") @RequestParam(required = false) Long categoryId,
      @Parameter(description = "컨디션 필터") @RequestParam(required = false) String condition,
      @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

    ProductSearchRequest searchRequest = ProductSearchRequest.builder()
        .keyword(keyword)
        .status(status != null ? com.oboe.backend.product.entity.ProductStatus.valueOf(
            status.toUpperCase()) : null)
        .sortBy(sortBy)
        .categoryId(categoryId)
        .condition(condition)
        .page(page)
        .size(size)
        .build();

    Page<ProductListResponse> response = productService.getProducts(searchRequest);

    return ResponseEntity.ok(ResponseDto.success("상품 목록을 성공적으로 조회했습니다.", response));
  }

  @GetMapping("/{productId}")
  @Operation(summary = "상품 상세 조회", description = "특정 상품의 상세 정보를 조회합니다. 조회수가 증가합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "상품 상세 조회 성공"),
      @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  public ResponseEntity<ResponseDto<ProductResponse>> getProduct(
      @Parameter(description = "상품 ID") @PathVariable Long productId) {

    ProductResponse response = productService.getProductDetail(productId);

    return ResponseEntity.ok(ResponseDto.success("상품 상세 정보를 성공적으로 조회했습니다.", response));
  }

  @GetMapping("/popular")
  @Operation(summary = "인기 상품 조회", description = "조회수 기준 인기 상품 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "인기 상품 조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  public ResponseEntity<ResponseDto<List<ProductListResponse>>> getPopularProducts(
      @Parameter(description = "조회할 상품 개수") @RequestParam(defaultValue = "10") int limit) {

    List<ProductListResponse> response = productService.getPopularProducts(limit);

    return ResponseEntity.ok(ResponseDto.success("인기 상품 목록을 성공적으로 조회했습니다.", response));
  }

  @GetMapping("/latest")
  @Operation(summary = "최신 상품 조회", description = "최신 등록 상품 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "최신 상품 조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  public ResponseEntity<ResponseDto<List<ProductListResponse>>> getLatestProducts(
      @Parameter(description = "조회할 상품 개수") @RequestParam(defaultValue = "10") int limit) {

    List<ProductListResponse> response = productService.getLatestProducts(limit);

    return ResponseEntity.ok(ResponseDto.success("최신 상품 목록을 성공적으로 조회했습니다.", response));
  }


}
