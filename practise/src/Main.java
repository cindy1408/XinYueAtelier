//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

void main() {
    //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
    // to see how IntelliJ IDEA suggests fixing it.

    //    Exercise 2 — Collections & streams
    //    Given a List<Specimen>, write a method that:
    //
    //    Filters to only specimens acquired before 1950
    //    Returns them sorted alphabetically by name
    //    Use streams
    List<Specimen> allSpecimen = List.of(
            new Specimen("Whale","Mammal", 1400 ),
            new Specimen("Dolphin","Mammal", 2000 ),
            new Specimen("Sea Ota","Mammal", 2020 ),
            new Specimen("Blue Whale","Mammal", 1500 ),
            new Specimen("Elephant","Mammal", 1850 )
            );


    List<Specimen> sortedSpecimen = allSpecimen
            .stream()
            .filter( s -> s.getYear() < 1950)
            .sorted(Comparator.comparing(Specimen::getName))
                    .toList();

    sortedSpecimen.forEach(specimen -> System.out.println(specimen.getSummary()));


    //    Exercise 3 — Interface
    //Create a Searchable interface with a method matchesQuery(String query).
    //
    //Have Specimen implement it — it should return true if the query matches the name or category (case-insensitive).


    Specimen whale = new Specimen("Blue Whale", "Mammal", 1938);

// You can call it directly on the object
    System.out.println(whale.matchesQuery("mammal")); // true
    System.out.println(whale.matchesQuery("Reptile")); // false

// Or via the interface type — this is the point of interfaces
    Searchable s = whale;
    System.out.println(s.matchesQuery("Blue Whale")); // true
}
