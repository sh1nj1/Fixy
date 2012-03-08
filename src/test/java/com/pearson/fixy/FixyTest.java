package com.pearson.fixy;

import com.fixy.JPAPersister;
import com.petstore.Order;
import com.petstore.Pet;
import com.petstore.PetType;
import com.petstore.users.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FixyTest {
    JPAPersister jpaPersister;
    Fixy fixtures;

    @Before public void setup() {
    	EntityManager petstore = Persistence.createEntityManagerFactory("petstore").createEntityManager();
        jpaPersister = new JPAPersister(petstore);
    	petstore.getTransaction().begin();
        fixtures = new Fixy(jpaPersister, "com.petstore");
    }

    @After public void tearDown() {
        jpaPersister.getEntityManager().getTransaction().rollback();
    }

    @Test public void testPetTypes() {
        fixtures.load("pet_types.yaml");

        PetType dog = jpaPersister.getEntityManager().createQuery(
                "select type from PetType type where type.name = 'Dog'", PetType.class)
                .getSingleResult();

        assertThat(dog.getName(), is("Dog"));
    }
    
    @Test public void testPets() {
        fixtures.load("pets.yaml");

        Pet fido = jpaPersister.getEntityManager().createQuery("select p from Pet p where p.name = 'Fido'", Pet.class).getSingleResult();

        assertThat(fido.getName(), is("Fido"));
        assertThat(fido.getType().getName(), is("Dog"));
        
    }

    @Test
    public void testOrders() {
        fixtures.load("orders.yaml");

        Order order = jpaPersister.getEntityManager().createQuery("select o from Order o where o.pet.name= 'Fido'", Order.class).getSingleResult();
        
        assertThat(order.getPet().getName(), is("Fido"));
    }
    
    @Test
    public void testAddress() {
        fixtures.load("address.yaml");

        Order order = jpaPersister.getEntityManager().createQuery("select o from Order o where o.pet.name= 'Fido'", Order.class).getSingleResult();
        
        assertThat(order.getAddress().getCity(), is("Paris"));
    }
    
    @Test
    public void testPostProcessor() {
        Processor<User> defaultPassword = new Processor<User>(User.class) {
            @Override public void process(User user) {
                user.setPassword("TEST_PASS");
            }
        };

        fixtures.addProcessor(defaultPassword);

        fixtures.load("users.yaml");

        User user = jpaPersister.getEntityManager().createQuery("select u from User u where u.name = 'George Washington'", User.class).getSingleResult();
        
        assertThat(user.getPassword(), is("TEST_PASS"));
    }
    
    @Test public void testPostProcessorCanCreateEntities() {
        Processor<Pet> createPetOwner = new Processor<Pet>(Pet.class) {
            @Override public void process(Pet pet) {
                User petOwner = new User();
                petOwner.setName(pet.getName());
                addEntity(petOwner);
            }
        };

        fixtures.addProcessor(createPetOwner);

        fixtures.load("pets.yaml");

        User petOwner = jpaPersister.getEntityManager().createQuery("select u from User u  where u.name = 'Fido'", User.class).getSingleResult();

        assertThat(petOwner.getName(), is("Fido"));
    }
}
