package com.tratif.migrationstestdemo;

import java.util.Collection;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

	@Id @GeneratedValue
	private Long id;
	
	private String name;
	
	@ManyToMany(mappedBy = "customers")
	private Collection<Order> orders;
}
