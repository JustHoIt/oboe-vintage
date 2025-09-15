package com.oboe.backend.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDeliveryInfo is a Querydsl query type for DeliveryInfo
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QDeliveryInfo extends BeanPath<DeliveryInfo> {

    private static final long serialVersionUID = 1714815195L;

    public static final QDeliveryInfo deliveryInfo = new QDeliveryInfo("deliveryInfo");

    public final DateTimePath<java.time.LocalDateTime> deliveredAt = createDateTime("deliveredAt", java.time.LocalDateTime.class);

    public final StringPath deliveryMemo = createString("deliveryMemo");

    public final StringPath detailAddress = createString("detailAddress");

    public final StringPath recipientName = createString("recipientName");

    public final StringPath recipientPhone = createString("recipientPhone");

    public final StringPath roadAddress = createString("roadAddress");

    public final StringPath trackingNumber = createString("trackingNumber");

    public final StringPath zipCode = createString("zipCode");

    public QDeliveryInfo(String variable) {
        super(DeliveryInfo.class, forVariable(variable));
    }

    public QDeliveryInfo(Path<? extends DeliveryInfo> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDeliveryInfo(PathMetadata metadata) {
        super(DeliveryInfo.class, metadata);
    }

}

