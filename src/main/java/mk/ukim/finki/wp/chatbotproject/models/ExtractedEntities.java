package mk.ukim.finki.wp.chatbotproject.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured entity payload returned by LLM extraction.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedEntities {

    private List<String> products = new ArrayList<>();
    private List<String> versions = new ArrayList<>();
    private List<String> people = new ArrayList<>();
    private List<String> locations = new ArrayList<>();
    private List<String> technologies = new ArrayList<>();
    private List<String> numbers = new ArrayList<>();
    private List<String> currencies = new ArrayList<>();

    public List<String> getProducts() {
        return products;
    }

    public void setProducts(List<String> products) {
        this.products = products != null ? products : new ArrayList<>();
    }

    public List<String> getVersions() {
        return versions;
    }

    public void setVersions(List<String> versions) {
        this.versions = versions != null ? versions : new ArrayList<>();
    }

    public List<String> getPeople() {
        return people;
    }

    public void setPeople(List<String> people) {
        this.people = people != null ? people : new ArrayList<>();
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations != null ? locations : new ArrayList<>();
    }

    public List<String> getTechnologies() {
        return technologies;
    }

    public void setTechnologies(List<String> technologies) {
        this.technologies = technologies != null ? technologies : new ArrayList<>();
    }

    public List<String> getNumbers() {
        return numbers;
    }

    public void setNumbers(List<String> numbers) {
        this.numbers = numbers != null ? numbers : new ArrayList<>();
    }

    public List<String> getCurrencies() {
        return currencies;
    }

    public void setCurrencies(List<String> currencies) {
        this.currencies = currencies != null ? currencies : new ArrayList<>();
    }
}
