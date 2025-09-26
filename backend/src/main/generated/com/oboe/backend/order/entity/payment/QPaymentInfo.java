package com.oboe.backend.order.entity.payment;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPaymentInfo is a Querydsl query type for PaymentInfo
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QPaymentInfo extends BeanPath<PaymentInfo> {

    private static final long serialVersionUID = 649135027L;

    public static final QPaymentInfo paymentInfo = new QPaymentInfo("paymentInfo");

    public final DateTimePath<java.time.LocalDateTime> approvedAt = createDateTime("approvedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> cancelledAt = createDateTime("cancelledAt", java.time.LocalDateTime.class);

    public final StringPath cancelReason = createString("cancelReason");

    public final StringPath cardCompany = createString("cardCompany");

    public final StringPath cardNumber = createString("cardNumber");

    public final StringPath customerKey = createString("customerKey");

    public final StringPath failUrl = createString("failUrl");

    public final StringPath installmentPlanMonths = createString("installmentPlanMonths");

    public final StringPath orderId = createString("orderId");

    public final StringPath orderName = createString("orderName");

    public final DateTimePath<java.time.LocalDateTime> paidAt = createDateTime("paidAt", java.time.LocalDateTime.class);

    public final StringPath paymentId = createString("paymentId");

    public final StringPath paymentKey = createString("paymentKey");

    public final EnumPath<PaymentMethod> paymentMethod = createEnum("paymentMethod", PaymentMethod.class);

    public final StringPath receiptUrl = createString("receiptUrl");

    public final StringPath successUrl = createString("successUrl");

    public final EnumPath<PaymentStatus> tossPaymentStatus = createEnum("tossPaymentStatus", PaymentStatus.class);

    public final NumberPath<Long> totalAmount = createNumber("totalAmount", Long.class);

    public final StringPath transactionId = createString("transactionId");

    public QPaymentInfo(String variable) {
        super(PaymentInfo.class, forVariable(variable));
    }

    public QPaymentInfo(Path<? extends PaymentInfo> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPaymentInfo(PathMetadata metadata) {
        super(PaymentInfo.class, metadata);
    }

}

