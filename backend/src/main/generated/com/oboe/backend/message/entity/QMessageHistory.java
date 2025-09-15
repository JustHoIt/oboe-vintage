package com.oboe.backend.message.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMessageHistory is a Querydsl query type for MessageHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMessageHistory extends EntityPathBase<MessageHistory> {

    private static final long serialVersionUID = -758419745L;

    public static final QMessageHistory messageHistory = new QMessageHistory("messageHistory");

    public final StringPath failureReason = createString("failureReason");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath message = createString("message");

    public final StringPath recipient = createString("recipient");

    public final DateTimePath<java.time.LocalDateTime> sendAt = createDateTime("sendAt", java.time.LocalDateTime.class);

    public final BooleanPath status = createBoolean("status");

    public QMessageHistory(String variable) {
        super(MessageHistory.class, forVariable(variable));
    }

    public QMessageHistory(Path<? extends MessageHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMessageHistory(PathMetadata metadata) {
        super(MessageHistory.class, metadata);
    }

}

