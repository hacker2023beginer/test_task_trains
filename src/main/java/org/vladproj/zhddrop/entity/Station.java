package org.vladproj.zhddrop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name="stations")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Station {
    @Id
    private String code;

    @Column(name = "name", nullable = false)
    private String name;
}
