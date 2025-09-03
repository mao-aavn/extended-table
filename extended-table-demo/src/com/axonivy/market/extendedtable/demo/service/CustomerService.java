package com.axonivy.market.extendedtable.demo.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.transaction.TransactionRolledbackException;

import org.primefaces.util.Constants;

import com.axonivy.market.extendedtable.demo.daos.CustomerDAO;
import com.axonivy.market.extendedtable.demo.entities.Country;
import com.axonivy.market.extendedtable.demo.entities.Customer;
import com.axonivy.market.extendedtable.demo.entities.CustomerGroup;
import com.axonivy.market.extendedtable.demo.entities.CustomerStatus;

import ch.ivyteam.ivy.environment.Ivy;

public class CustomerService {

	private Random random = new SecureRandom();
	private Country[] countries;
	private String[] firstNames;
	private String[] lastNames;
	private String[] companies;

	private CustomerDAO customerDAO = new CustomerDAO();

	{
		countries = new Country[] { new Country("Argentina", "ar"), new Country("Australia", "au"),
				new Country("Brazil", "br"), new Country("Canada", "ca"), new Country("Germany", "de"),
				new Country("France", "fr"), new Country("India", "in"), new Country("Italy", "it"),
				new Country("Japan", "jp"), new Country("Russia", "ru"), new Country("Spain", "es"),
				new Country("United Kingdom", "gb") };

		companies = new String[] { "Benton, John B Jr", "Chanay, Jeffrey A Esq", "Chemel, James L Cpa",
				"Feltz Printing Service", "Printing Dimensions", "Chapman, Ross E Esq", "Morlong Associates",
				"Commercial Press", "Truhlar And Truhlar Attys", "King, Christopher A Esq", "Dorl, James J Esq",
				"Rangoni Of Florence", "Feiner Bros", "Buckley Miller Wright", "Rousseaux, Michael Esq" };

		firstNames = new String[] { "James", "David", "Jeanfrancois", "Ivar", "Tony", "AdAMS", "Claire", "Costa",
				"Juan", "Maria", "Jennifer", "Stacey", "Leja", "Morrow", "Arvin", "Darci", "Izzy", "Ricardo",
				"Clifford", "Emily", "Kadeem", "Mujtaba", "Aika", "Mayumi", "Misaki", "Silvio", "Nicolas", "Antonio",
				"Deepesh", "Aditya", "Aruna", "Jones", "Julie", "Smith", "Johnson", "Francesco", "Salvatore", "Kaitlin",
				"Faith", "Maisha", "Jefferson", "Leon", "Rodrigues", "Alejandro", "Munro", "Cody", "Chavez", "Sinclair",
				"Isabel", "Octavia", "Murillo", "Greenwood", "Wickens", "Ashley" };
		lastNames = new String[] { "Butt", "Darakjy", "Venere", "Paprocki", "Foller", "Morasca", "Tollner", "Dilliard",
				"Wieser", "Marrier", "Amigon", "Maclead", "Caldarera", "Ruta", "Albares", "Poquette", "Garufi",
				"Gaucho", "Rim", "Whobrey", "Flosi", "Nicka", "Inouye", "Kolmetz", "Royster", "Slusarski", "Iturbide",
				"Caudy", "Chui", "Kusko", "Figeroa", "Vocelka", "Stenseth", "Glick", "Sergi", "Shinko", "Stockham",
				"Ostrosky", "Gillian", "Rulapaugh", "Schemmer", "Oldroyd", "Campain", "Perin", "Ferencz", "Saylors",
				"Briddick", "Waycott", "Bowley", "Malet", "Malet", "Bolognia", "Nestle", "Doe" };
	}

	public void initCustomersIfNotExisting(int number) {

		if (!customerDAO.findAll().isEmpty()) {
			return;
		}

		List<Customer> customers = new ArrayList<>();
		for (int i = 0; i < number; i++) {
			customers.add(Customer.builder().name(getName()).company(getCompany()).country(getCountry()).date(getDate())
					.status(CustomerStatus.random()).rank(getRank()).isNew(random.nextBoolean()).build());
		}
		assignGroupsToCustomers(customers);

		try {
			Ivy.log().info(customers.get(0));
			customers = customerDAO.saveAll(customers);
			Ivy.log().info("INIT COMPLETED: Records: " + customers.size());
		} catch (TransactionRolledbackException e) {
			Ivy.log().error(e);
		}

	}

	private void assignGroupsToCustomers(List<Customer> customers) {
		CustomerGroup groupA = new CustomerGroup("Enterprise Customers");
		groupA.setDescription("Large enterprise clients");
		groupA.setLeader("Alice Smith");

		CustomerGroup groupB = new CustomerGroup("SMB Customers");
		groupB.setDescription("Small and medium businesses");
		groupB.setLeader("Bob Johnson");

		CustomerGroup groupC = new CustomerGroup("VIP Customers");
		groupC.setDescription("High value VIP clients");
		groupC.setLeader("Carol White");

		// Example: Assign groups to customers during initialization
		for (int i = 0; i < customers.size(); i++) {
			Customer customer = customers.get(i);
			if (i % 3 == 0) {
				customer.setGroup(groupA);
				// groupA.getCustomers().add(customer);
			} else if (i % 3 == 1) {
				customer.setGroup(groupB);
				// groupB.getCustomers().add(customer);
			} else {
				customer.setGroup(groupC);
				// groupC.getCustomers().add(customer);
			}
		}
	}

	public List<Country> getCountries() {
		return Arrays.asList(countries);
	}

	public CustomerStatus[] getCustomerStatus() {
		return CustomerStatus.values();
	}

	private String getName() {
		return firstNames[random.nextInt(firstNames.length)] + Constants.SPACE + (char) (random.nextInt(26) + 'A')
				+ Constants.SPACE + lastNames[random.nextInt(lastNames.length)];
	}

	private Country getCountry() {
		return countries[random.nextInt(countries.length)];
	}

	private String getCompany() {
		return companies[random.nextInt(companies.length)];
	}

	private LocalDate getDate() {
		LocalDate now = LocalDate.now();
		long randomDay = ThreadLocalRandom.current().nextLong(now.minusDays(30).toEpochDay(), now.toEpochDay());
		return LocalDate.ofEpochDay(randomDay);
	}

	private int getRank() {
		return random.nextInt(100);
	}

	public List<Customer> findAll() {
		return customerDAO.findAll();
	}
}
