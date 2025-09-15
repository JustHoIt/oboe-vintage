package com.oboe.backend.product.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProduct is a Querydsl query type for Product
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProduct extends EntityPathBase<Product> {

    private static final long serialVersionUID = 266056437L;

    public static final QProduct product = new QProduct("product");

    public final com.oboe.backend.common.domain.QBaseTimeEntity _super = new com.oboe.backend.common.domain.QBaseTimeEntity(this);

    public final StringPath brand = createString("brand");

    public final SetPath<ProductCategory, QProductCategory> categories = this.<ProductCategory, QProductCategory>createSet("categories", ProductCategory.class, QProductCategory.class, PathInits.DIRECT2);

    public final EnumPath<Condition> condition = createEnum("condition", Condition.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final NumberPath<java.math.BigDecimal> price = createNumber("price", java.math.BigDecimal.class);

    public final ListPath<ProductImage, QProductImage> productImages = this.<ProductImage, QProductImage>createList("productImages", ProductImage.class, QProductImage.class, PathInits.DIRECT2);

    public final EnumPath<ProductStatus> productStatus = createEnum("productStatus", ProductStatus.class);

    public final StringPath size = createString("size");

    public final StringPath sku = createString("sku");

    public final NumberPath<Integer> stockQuantity = createNumber("stockQuantity", Integer.class);

    public final StringPath texture = createString("texture");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Integer> views = createNumber("views", Integer.class);

    public final StringPath yearOfRelease = createString("yearOfRelease");

    public QProduct(String variable) {
        super(Product.class, forVariable(variable));
    }

    public QProduct(Path<? extends Product> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProduct(PathMetadata metadata) {
        super(Product.class, metadata);
    }

}

