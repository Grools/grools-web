package fr.cea.ig.grools.server;


import com.google.gson.Gson;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public class AvailableTags {
    private final String[] species;
    private final Map<String,List<String>> strains;

    public AvailableTags( @NotNull final String[] species, @NotNull final Map<String, List<String>> strains) {
        this.species        = species;
        this.strains        = strains;
    }


    public String[] getSpecies() {
        return species;
    }

    public Map<String,List<String>> getStrains() {
        return strains;
    }

    public List<String> getStrains(@NotNull final String species) {
        return strains.get(species);
    }

    @Override
    public String toString(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
