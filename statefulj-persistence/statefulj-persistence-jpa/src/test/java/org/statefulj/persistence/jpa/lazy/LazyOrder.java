package org.statefulj.persistence.jpa.lazy;

import org.statefulj.persistence.annotations.State;
import org.statefulj.persistence.jpa.model.StatefulEntity;

import javax.persistence.*;

@Entity
@Table(name = "LazyOrders")
public class LazyOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(unique = true, nullable = false)
    private long id;

    @State
    @Column(insertable=true, updatable=false)
    private String state;

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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
