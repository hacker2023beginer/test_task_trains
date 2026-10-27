package org.vladproj.zhddrop.entity;

import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name="stations")
public class Station {
    @Id
    private String code;

    @Column(name = "name", nullable = false)
    private String name;
}
