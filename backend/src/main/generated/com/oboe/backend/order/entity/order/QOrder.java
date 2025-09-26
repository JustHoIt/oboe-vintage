package com.oboe.backend.order.entity.order;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrder is a Querydsl query type for Order
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrder extends EntityPathBase<Order> {

    private static final long serialVersionUID = 288056181L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrder order = new QOrder("order1");

    public final com.oboe.backend.common.domain.QBaseTimeEntity _super = new com.oboe.backend.common.domain.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<java.math.BigDecimal> deliveryFee = createNumber("deliveryFee", java.math.BigDecimal.class);

    public final com.oboe.backend.order.entity.QDeliveryInfo deliveryInfo;

    public final NumberPath<java.math.BigDecimal> discountAmount = createNumber("discountAmount", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> finalAmount = createNumber("finalAmount", java.math.BigDecimal.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<OrderItem, QOrderItem> orderItems = this.<OrderItem, QOrderItem>createList("orderItems", OrderItem.class, QOrderItem.class, PathInits.DIRECT2);

    public final StringPath orderNumber = createString("orderNumber");

    public final com.oboe.backend.order.entity.payment.QPaymentInfo paymentInfo;

    public final EnumPath<com.oboe.backend.order.entity.payment.PaymentMethod> paymentMethod = createEnum("paymentMethod", com.oboe.backend.order.entity.payment.PaymentMethod.class);

    public final EnumPath<OrderStatus> status = createEnum("status", OrderStatus.class);

    public final ListPath<OrderStatusHistory, QOrderStatusHistory> statusHistory = this.<OrderStatusHistory, QOrderStatusHistory>createList("statusHistory", OrderStatusHistory.class, QOrderStatusHistory.class, PathInits.DIRECT2);

    public final NumberPath<java.math.BigDecimal> totalAmount = createNumber("totalAmount", java.math.BigDecimal.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.oboe.backend.user.entity.QUser user;

    public QOrder(String variable) {
        this(Order.class, forVariable(variable), INITS);
    }

    public QOrder(Path<? extends Order> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrder(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrder(PathMetadata metadata, PathInits inits) {
        this(Order.class, metadata, inits);
    }

    public QOrder(Class<? extends Order> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.deliveryInfo = inits.isInitialized("deliveryInfo") ? new com.oboe.backend.order.entity.QDeliveryInfo(forProperty("deliveryInfo")) : null;
        this.paymentInfo = inits.isInitialized("paymentInfo") ? new com.oboe.backend.order.entity.payment.QPaymentInfo(forProperty("paymentInfo")) : null;
        this.user = inits.isInitialized("user") ? new com.oboe.backend.user.entity.QUser(forProperty("user")) : null;
    }

}

