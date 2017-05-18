package fr.cea.ig.grools.server;


import javax.json.Json;
import javax.json.JsonObject;
import javax.validation.constraints.NotNull;


public class UserData {
    private final String species;
    private final Boolean speciesSelected;
    private final String strains;
    private final Boolean strainsSelected;
    private final String mode;
    private final String metabolicNetworkModel;

    public UserData(@NotNull final String species, @NotNull final Boolean speciesSelected, @NotNull final String strains, @NotNull final Boolean strainsSelected, @NotNull final String mode, @NotNull final String metabolicNetworkModel) {
        this.species                = species;
        this.speciesSelected        = speciesSelected;
        this.strains                = strains;
        this.strainsSelected        = strainsSelected;
        this.mode                   = mode;
        this.metabolicNetworkModel  = metabolicNetworkModel;
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

    public String getMode(){
        return mode;
    }

    public String getMetabolicNetworkModel(){
        return metabolicNetworkModel;
    }

    @Override
    public String toString(){
        JsonObject jsonObject = Json.createObjectBuilder()
                                    .add("species", species)
                                    .add("speciesSelected", speciesSelected)
                                    .add("strains", strains )
                                    .add("strainsSelected", strainsSelected)
                                    .add("mode", mode)
                                    .add("metabolicNetworkModel", metabolicNetworkModel)
                                    .build();
        return jsonObject.toString();
    }
}
