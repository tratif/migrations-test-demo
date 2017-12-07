package com.tratif.migrationstestdemo;

import java.util.Collection;

import javax.annotation.Generated;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

	@Id @GeneratedValue
	private Long id;
	
	private String itemName;
	
	@ManyToMany
	private Collection<Customer> customers;
	
}
