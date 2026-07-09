import java.util.List;

public class Specimen implements Searchable {
    //Exercise 1 — Classes & encapsulation
//Model a Specimen (as in a museum specimen). It should have:
//
//        A name, a category (e.g. "Mammal"), and a year it was acquired
//A method getSummary() that returns a formatted string like:
//
//        "Blue Whale [Mammal] — acquired 1938"
//The year should not be settable after construction
    private String name;
    private String category;
    private final int year;

    public Specimen(String name, String category, int year) {
        this.name = name;
        this.category = category;
        this.year = year;
    }

    public String getName() {
        return this.name;
    }

    public String getCategory() {
        return category;
    }

    public int getYear() {
        return year;
    }

    @Override
    public String toString() {
        return "Specimen{" +
                "name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", year=" + year +
                '}';
    }

    public String getSummary() {
        return name + " [" + category + "] — acquired " + year;
    }

    //    Create a Searchable interface with a method matchesQuery(String query).
//    Have Specimen implement it — it should return true if the query matches the name or category (case-insensitive).


    @Override
    public boolean matchesQuery(String query) {
        return this.name.contains(query) ||
                this.category.contains(query);
    }
}



//🔬 Technical Briefing — Workshop Challenge (Day 4)
//
//You are joining the NHM digital collections team. The museum holds over 80 million specimens, and curators currently search for them via a legacy system. You have been asked to design and partially implement a simple Specimen Search API in Java/Spring Boot.
//The API should allow:
//
//Searching specimens by keyword (name or category)
//Retrieving a specimen by ID
//Adding a new specimen
//
//You will be expected to walk through your design, write key parts of the code, and discuss any trade-offs you made.
//
//Sit with that — we'll do the full mock on Day 4. For now, just let it percolate while you work on the warm-ups. 🙂