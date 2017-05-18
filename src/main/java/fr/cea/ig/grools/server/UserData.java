package fr.cea.ig.grools.server;


import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.validation.constraints.NotNull;
import fr.cea.ig.grools.reasoner.Mode;
import fr.cea.ig.grools.reasoner.VariantMode;

import java.util.Set;
import java.util.stream.Collectors;


public class UserData {
    private final String species;
    private final Boolean speciesSelected;
    private final String strains;
    private final Boolean strainsSelected;
    private final String mode;
    private final String metabolicNetworkModel;

    private String getMode(@NotNull final JsonArray modes){
        Set<VariantMode> variantModes = modes.stream( )
                                             .map( i -> VariantMode.valueOf( i.toString( ) ) )
                                             .collect( Collectors.toSet( ) );
        // TODO build mode from variant mode into grools-reasoner
        StringBuilder sb = new StringBuilder( "normal" ); // TODO Mode class from grools-reasoner need to be rewritten
        if( variantModes.contains( VariantMode.SPECIFIC  ) )
            sb.append( "-specific" );
        return sb.toString();
    }

    public UserData( @NotNull final String species, @NotNull final Boolean speciesSelected, @NotNull final String strains, @NotNull final Boolean strainsSelected, @NotNull final JsonArray modes, @NotNull final String metabolicNetworkModel ) {
        this.species                = species;
        this.speciesSelected        = speciesSelected;
        this.strains                = strains;
        this.strainsSelected        = strainsSelected;
        this.mode                   = getMode(modes);
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
