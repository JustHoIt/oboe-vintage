package com.oboe.backend.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrderStatusHistory is a Querydsl query type for OrderStatusHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrderStatusHistory extends EntityPathBase<OrderStatusHistory> {

    private static final long serialVersionUID = -1843323155L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrderStatusHistory orderStatusHistory = new QOrderStatusHistory("orderStatusHistory");

    public final com.oboe.backend.common.domain.QBaseTimeEntity _super = new com.oboe.backend.common.domain.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final EnumPath<OrderStatus> fromStatus = createEnum("fromStatus", OrderStatus.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath memo = createString("memo");

    public final QOrder order;

    public final StringPath reason = createString("reason");

    public final EnumPath<OrderStatus> toStatus = createEnum("toStatus", OrderStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QOrderStatusHistory(String variable) {
        this(OrderStatusHistory.class, forVariable(variable), INITS);
    }

    public QOrderStatusHistory(Path<? extends OrderStatusHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrderStatusHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrderStatusHistory(PathMetadata metadata, PathInits inits) {
        this(OrderStatusHistory.class, metadata, inits);
    }

    public QOrderStatusHistory(Class<? extends OrderStatusHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.order = inits.isInitialized("order") ? new QOrder(forProperty("order"), inits.get("order")) : null;
    }

}

