package com.axonivy.market.extendedtable.demo.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
	private String[] cities;
	private String[] streets;
	private String[] departments;
	private String[] domains;

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
		
		cities = new String[] { "New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia", 
				"San Antonio", "San Diego", "Dallas", "San Jose", "Austin", "Jacksonville", "Fort Worth", 
				"Columbus", "Charlotte", "Seattle", "Denver", "Boston", "Portland", "Miami" };
		
		streets = new String[] { "Main St", "Oak Ave", "Pine Rd", "Maple Dr", "Cedar Ln", "Elm St", 
				"Washington Blvd", "Park Ave", "Broadway", "Lake St", "Hill Rd", "Forest Dr", "River Rd", 
				"Sunset Blvd", "Ocean Ave", "Market St", "Church St", "School Rd", "Mill St", "Spring St" };
		
		departments = new String[] { "Sales", "Marketing", "Engineering", "Human Resources", "Finance", 
				"Operations", "Customer Service", "IT", "Legal", "Product Management", "Research", 
				"Quality Assurance", "Business Development", "Procurement", "Administration" };
		
		domains = new String[] { "example.com", "business.com", "company.net", "enterprise.org", 
				"solutions.io", "tech.co", "global.com", "services.net", "consulting.com", "industries.com" };
	}

	public void initCustomersIfNotExisting(int number) {

		if (!customerDAO.findAll().isEmpty()) {
			return;
		}

		List<Customer> customers = new ArrayList<>();
		for (int i = 0; i < number; i++) {
			customers.add(Customer.builder()
					.name(getName())
					.company(getCompany())
					.country(getCountry())
					.date(getDate())
					.date1(getDate())
					.dateTime(getDateTime())
					.status(CustomerStatus.random())
					.income(random.nextDouble() * 1000000)
					.rank(getRank())
					.email(getEmail())
					.phone(getPhone())
					.address(getAddress())
					.city(getCity())
					.postalCode(getPostalCode())
					.website(getWebsite())
					.description(getDescription())
					.notes(getNotes())
					.contactPerson(getContactPerson())
					.department(getDepartment())
					.hasRepresentative(random.nextBoolean()).build());
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
	    long randomDay = ThreadLocalRandom.current()
	            .nextLong(now.minusDays(30).toEpochDay(), now.toEpochDay() + 1);

	    return LocalDate.ofEpochDay(randomDay);
	}
	
	private LocalDateTime getDateTime() {
		LocalDate now = LocalDate.now();
		long randomDay = ThreadLocalRandom.current()
				.nextLong(now.minusDays(30).toEpochDay(), now.toEpochDay() + 1);
		
		LocalDate randomDate = LocalDate.ofEpochDay(randomDay);
		return randomDate.atStartOfDay(); // or add random time if you wish
	}

	private int getRank() {
		return random.nextInt(100);
	}
	
	private String getEmail() {
		return firstNames[random.nextInt(firstNames.length)].toLowerCase() + "." + 
				lastNames[random.nextInt(lastNames.length)].toLowerCase() + "@" + 
				domains[random.nextInt(domains.length)];
	}
	
	private String getPhone() {
		return String.format("+1-%03d-%03d-%04d", 
				random.nextInt(1000), 
				random.nextInt(1000), 
				random.nextInt(10000));
	}
	
	private String getAddress() {
		return (random.nextInt(9999) + 1) + " " + streets[random.nextInt(streets.length)];
	}
	
	private String getCity() {
		return cities[random.nextInt(cities.length)];
	}
	
	private String getPostalCode() {
		return String.format("%05d", random.nextInt(100000));
	}
	
	private String getWebsite() {
		return "www." + companies[random.nextInt(companies.length)]
				.replaceAll("[^a-zA-Z0-9]", "")
				.toLowerCase() + ".com";
	}
	
	private String getDescription() {
		String[] descriptions = {
			"Leading provider of innovative solutions",
			"Industry expert with 20+ years experience",
			"Trusted partner for global enterprises",
			"Specialized in cutting-edge technology",
			"Award-winning service provider"
		};
		return descriptions[random.nextInt(descriptions.length)];
	}
	
	private String getNotes() {
		String[] notes = {
			"Important client - high priority",
			"Follow up needed next quarter",
			"Recently renewed contract",
			"Interested in premium services",
			"Request quarterly business review"
		};
		return notes[random.nextInt(notes.length)];
	}
	
	private String getContactPerson() {
		return firstNames[random.nextInt(firstNames.length)] + " " + 
				lastNames[random.nextInt(lastNames.length)];
	}
	
	private String getDepartment() {
		return departments[random.nextInt(departments.length)];
	}

	public List<Customer> findAll() {
		return customerDAO.findAll();
	}
}
