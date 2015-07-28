package fr.cea.ig.grools.server;


import javax.json.Json;
import javax.json.JsonObject;
import javax.validation.constraints.NotNull;


public class UserData {
    private final String species;
    private final Boolean speciesSelected;
    private final String strains;
    private final Boolean strainsSelected;

    public UserData(@NotNull final String species, @NotNull final Boolean speciesSelected, @NotNull final String strains, @NotNull final Boolean strainsSelected) {
        this.species            = species;
        this.speciesSelected    = speciesSelected;
        this.strains            = strains;
        this.strainsSelected    = strainsSelected;
    }

    public String getSpecies() {
        return species;
    }

    public String getStrains() {
        return strains;
    }

    public Boolean getSpeciesSelected() {
        return speciesSelected;
    }

    public Boolean getStrainsSelected() {
        return strainsSelected;
    }

    @Override
    public String toString(){
        JsonObject jsonObject = Json.createObjectBuilder()
                                    .add("species", species)
                                    .add("speciesSelected", speciesSelected)
                                    .add("strains", strains )
                                    .add("strainsSelected", strainsSelected)
                                    .build();
        return jsonObject.toString();
    }
}
