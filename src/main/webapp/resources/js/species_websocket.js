

function initUserData(){
    return  { 'species': '', 'strains': '', 'speciesSelected': false, 'strainsSelected': false };
}

var grools_svg;
var svgDoc;
var speciesInput;
var strainsInput;
var userData     = initUserData();
var availableTags= { 'species': [], 'strains': [], svg: '', 'eventNodes': [] };
var organismWS;

function reset(){
    speciesInput.prop('readonly', false);
    strainsInput.prop('readonly', false);
    speciesInput.val('');
    strainsInput.val('');
    grools_svg.prop('data', '/resources/svg/default.svg' );
    userData=initUserData();
}

$( document ).ready(function() {
    speciesInput        = $('#species');
    strainsInput        = $('#strains');
    grools_svg          = $('#grools_svg');
    const tooltips      = document.createElement('div');
    tooltips.id         = 'tooltips-content';
    tooltips.className  = 'grools';
    document.body.appendChild(tooltips);
    const tooltipsId    = document.getElementById('tooltips-content');
    availableTags= { 'species': [], 'strains': [], svg: '' };
    organismWS   = new WebSocket('ws://' + window.location.host + window.location.pathname + 'organisms');
    organismWS.onconnect = function(e) {
        console.log('connected');
    };
    organismWS.onerror = function (error) {
        console.log('WebSocket Error ' + error);
    };
    organismWS.onclose = function(event){
        console.log('Remote host closed or refused WebSocket connection');
        console.log(event);
    };
    organismWS.onmessage = function(message) {
        $(function() {
            availableTags = JSON.parse(message.data);
            console.log('species: ' + availableTags.species);
            console.log(JSON.stringify(availableTags.strains));
            if (!userData.speciesSelected) {
                speciesInput.autocomplete('option', {source: availableTags.species});
            }
            strainsInput.autocomplete('option', {source: availableTags.strains[speciesInput.val()]});
            if (availableTags.svg != '' ) {
                grools_svg.prop('data', availableTags.svg );
                //grools_svg.ready(function() {svgDoc = grools_svg[0].contentDocument;});
                while( svgDoc == null )
                    svgDoc = grools_svg[0].contentDocument;
                for (var i = 0; i < availableTags.eventNodes.length; i++) {
                    const item = availableTags.eventNodes[i];
                    addNodeEvent(tooltipsId, svgDoc, item[0], item[1], item[2]);
                }
                availableTags.eventNodes = []; // clear
            }
        });
    };
    speciesInput.keyup(  function(event) {
        event.preventDefault();
        var speciesText = speciesInput.val();
        userData.speciesSelected = false;
        if( strainsInput.val() != '' ){
            strainsInput.val('');
            userData.strains = '';
        }
        if (speciesText.length >= 3) {
            userData.species = speciesText;
            console.log(userData);
            organismWS.send(JSON.stringify(userData));
        }
    });
    strainsInput.keyup(  function(event) {
        event.preventDefault();
        var strainsText = strainsInput.val();
        if (strainsText.length > 3) {
            userData.strains = strainsText;
            console.log(userData);
            organismWS.send(JSON.stringify(userData));
        }
    });
    speciesInput.autocomplete({
        source: availableTags.species,
        select: function( event, ui ) {
            speciesInput.val( ui.item.label );
            strainsInput.val('');
            userData.species  = ui.item.label;
            if( ! speciesInput.prop('readonly') )
                userData.strains  = '';
            userData.speciesSelected = true;
            speciesInput.prop('readonly', true);
            userData.speciesSelected = true;
            organismWS.send(JSON.stringify(userData));
            return false;
        },
        minLength: 3
    });
    strainsInput.autocomplete({
        source: availableTags.strains[speciesInput.val()],
        select: function( event, ui ) {
            strainsInput.val( ui.item.label );
            strainsInput.prop('readonly', true);
            userData.strains            = ui.item.label;
            userData.strainsSelected    = true;
            organismWS.send(JSON.stringify(userData));
            return false;
        },
        minLength: 0
    });
    speciesInput.focus(function(){
        if ($(this).value == '')
        {
            $(this).autocomplete('search');
        }
        else
        {
            $(this).autocomplete('search', $(this).value);
        }
    });
    strainsInput.focus(function(){
        if ($(this).value == '')
        {
            $(this).autocomplete('search');
        }
        else
        {
            $(this).autocomplete('search', $(this).value);
        }
    });
});