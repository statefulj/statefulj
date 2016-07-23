package org.statefulj.persistence.jpa.lazy;

import org.statefulj.persistence.jpa.model.StatefulEntity;

import javax.persistence.*;

@Entity
@Table(name = "LazyOrders")
public class LazyOrder extends StatefulEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(unique = true, nullable = false)
    private long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    private LazyOrder order;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LazyOrder getOrder() {
        return order;
    }

    public void setOrder(LazyOrder order) {
        this.order = order;
    }
}
