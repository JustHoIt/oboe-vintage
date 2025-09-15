package com.oboe.backend.order.entity;

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

    private static final long serialVersionUID = -121910629L;

    public static final QPaymentInfo paymentInfo = new QPaymentInfo("paymentInfo");

    public final DateTimePath<java.time.LocalDateTime> cancelledAt = createDateTime("cancelledAt", java.time.LocalDateTime.class);

    public final StringPath cancelReason = createString("cancelReason");

    public final DateTimePath<java.time.LocalDateTime> paidAt = createDateTime("paidAt", java.time.LocalDateTime.class);

    public final StringPath paymentId = createString("paymentId");

    public final EnumPath<PaymentStatus> paymentStatus = createEnum("paymentStatus", PaymentStatus.class);

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

