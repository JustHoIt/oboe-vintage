package com.oboe.backend.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oboe.backend.product.dto.request.ProductCreateRequest;
import com.oboe.backend.product.dto.request.ProductSearchRequest;
import com.oboe.backend.product.dto.request.ProductUpdateRequest;
import com.oboe.backend.product.dto.response.ProductListResponse;
import com.oboe.backend.product.dto.response.ProductResponse;
import com.oboe.backend.product.entity.Condition;
import com.oboe.backend.product.entity.ProductStatus;
import com.oboe.backend.product.service.ProductService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@DisplayName("ProductController 테스트")
class ProductControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ProductService productService;


  private ProductCreateRequest createRequest;
  private ProductUpdateRequest updateRequest;
  private ProductResponse productResponse;
  private ProductListResponse listResponse;

  @BeforeEach
  void setUp() {
    // 상품 생성 요청 DTO
    createRequest = ProductCreateRequest.builder()
        .name("테스트 상품")
        .description("테스트 상품 설명")
        .categoryIds(Set.of(1L))
        .price(new BigDecimal("100000"))
        .stockQuantity(10)
        .productStatus(ProductStatus.ACTIVE)
        .brand("테스트브랜드")
        .condition(Condition.EXCELLENT)
        .build();

    // 상품 수정 요청 DTO
    updateRequest = ProductUpdateRequest.builder()
        .name("수정된 상품")
        .description("수정된 설명")
        .price(new BigDecimal("150000"))
        .build();

    // 상품 응답 DTO
    productResponse = ProductResponse.builder()
        .id(1L)
        .name("테스트 상품")
        .description("테스트 상품 설명")
        .price(new BigDecimal("100000"))
        .stockQuantity(10)
        .productStatus(ProductStatus.ACTIVE)
        .brand("테스트브랜드")
        .condition(Condition.EXCELLENT)
        .views(0)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    // 상품 목록 응답 DTO
    listResponse = ProductListResponse.builder()
        .id(1L)
        .name("테스트 상품")
        .price(new BigDecimal("100000"))
        .productStatus(ProductStatus.ACTIVE)
        .brand("테스트브랜드")
        .condition(Condition.EXCELLENT)
        .views(0)
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("상품 생성 API 테스트 - 성공")
  void createProduct_Success() throws Exception {
    // given
    given(productService.createProduct(any(ProductCreateRequest.class), any())).willReturn(
        productResponse);

    // when & then
    mockMvc.perform(post("/api/products")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("상품이 성공적으로 생성되었습니다."))
        .andExpect(jsonPath("$.data.id").value(1L))
        .andExpect(jsonPath("$.data.name").value("테스트 상품"));

    verify(productService).createProduct(any(ProductCreateRequest.class), any());
  }

  @Test
  @WithMockUser(roles = "USER")
  @DisplayName("상품 생성 API 테스트 - 권한 없음")
  void createProduct_Forbidden() throws Exception {
    // when & then
    mockMvc.perform(post("/api/products")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isForbidden());

    verify(productService, never()).createProduct(any(ProductCreateRequest.class), any());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("상품 수정 API 테스트 - 성공")
  void updateProduct_Success() throws Exception {
    // given
    given(productService.updateProduct(eq(1L), any(ProductUpdateRequest.class), any())).willReturn(
        productResponse);

    // when & then
    mockMvc.perform(put("/api/products/1")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("상품이 성공적으로 수정되었습니다."))
        .andExpect(jsonPath("$.data.id").value(1L));

    verify(productService).updateProduct(eq(1L), any(ProductUpdateRequest.class), any());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("상품 삭제 API 테스트 - 성공")
  void deleteProduct_Success() throws Exception {
    // given
    doNothing().when(productService).deleteProduct(eq(1L), any());

    // when & then
    mockMvc.perform(delete("/api/products/1")
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("상품이 성공적으로 삭제되었습니다."));

    verify(productService).deleteProduct(eq(1L), any());
  }

  @Test
  @WithMockUser
  @DisplayName("상품 목록 조회 API 테스트 - 전체 조회")
  void getProducts_AllProducts() throws Exception {
    // given
    Page<ProductListResponse> productPage = new PageImpl<>(List.of(listResponse));
    given(productService.getProducts(any(ProductSearchRequest.class))).willReturn(productPage);

    // when & then
    mockMvc.perform(get("/api/products")
            .param("page", "0")
            .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content[0].id").value(1L))
        .andExpect(jsonPath("$.data.content[0].name").value("테스트 상품"));

    verify(productService).getProducts(any(ProductSearchRequest.class));
  }

  @Test
  @WithMockUser
  @DisplayName("상품 목록 조회 API 테스트 - 키워드 검색")
  void getProducts_WithKeyword() throws Exception {
    // given
    Page<ProductListResponse> productPage = new PageImpl<>(List.of(listResponse));
    given(productService.getProducts(any(ProductSearchRequest.class))).willReturn(productPage);

    // when & then
    mockMvc.perform(get("/api/products")
            .param("keyword", "테스트")
            .param("page", "0")
            .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content").isArray());

    verify(productService).getProducts(argThat(request ->
        "테스트".equals(request.getKeyword())));
  }

  @Test
  @WithMockUser
  @DisplayName("상품 목록 조회 API 테스트 - 필터 조건")
  void getProducts_WithFilters() throws Exception {
    // given
    Page<ProductListResponse> productPage = new PageImpl<>(List.of(listResponse));
    given(productService.getProducts(any(ProductSearchRequest.class))).willReturn(productPage);

    // when & then
    mockMvc.perform(get("/api/products")
            .param("status", "ACTIVE")
            .param("sortBy", "latest")
            .param("brand", "테스트브랜드")
            .param("page", "0")
            .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(productService).getProducts(argThat(request ->
        ProductStatus.ACTIVE.equals(request.getStatus()) &&
            "latest".equals(request.getSortBy()) &&
            "테스트브랜드".equals(request.getBrand())));
  }

  @Test
  @WithMockUser
  @DisplayName("상품 상세 조회 API 테스트 - 성공")
  void getProduct_Success() throws Exception {
    // given
    given(productService.getProductDetail(1L)).willReturn(productResponse);

    // when & then
    mockMvc.perform(get("/api/products/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("상품 상세 정보를 성공적으로 조회했습니다."))
        .andExpect(jsonPath("$.data.id").value(1L))
        .andExpect(jsonPath("$.data.name").value("테스트 상품"));

    verify(productService).getProductDetail(1L);
  }

  @Test
  @WithMockUser
  @DisplayName("인기 상품 조회 API 테스트")
  void getPopularProducts() throws Exception {
    // given
    List<ProductListResponse> products = List.of(listResponse);
    given(productService.getPopularProducts(10)).willReturn(products);

    // when & then
    mockMvc.perform(get("/api/products/popular")
            .param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].id").value(1L));

    verify(productService).getPopularProducts(10);
  }

  @Test
  @WithMockUser
  @DisplayName("최신 상품 조회 API 테스트")
  void getLatestProducts() throws Exception {
    // given
    List<ProductListResponse> products = List.of(listResponse);
    given(productService.getLatestProducts(10)).willReturn(products);

    // when & then
    mockMvc.perform(get("/api/products/latest")
            .param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].id").value(1L));

    verify(productService).getLatestProducts(10);
  }

}
