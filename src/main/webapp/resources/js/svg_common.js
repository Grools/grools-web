
function tooltips_event( tooltips, id, node ){
    id.addEventListener( "mouseenter",  function( event ) {
        tooltips.appendChild( node );
        tooltipsPosition( event, tooltips );
        tooltips.style.display = 'block';

    } );
    id.addEventListener( "mouseleave",  function( event ) {
        tooltips.style.display = 'none';
        tooltips.removeChild(tooltips.firstChild);
    } );
    id.addEventListener( "mousemove",  function( event ) {
        tooltipsPosition( event, tooltips );
    } );
}
//TODO remove event

function createInformativeNode( text, color ){
    var div = document.createElement('div');
    var p   = document.createElement('p');
    p.style.color = color;
    p.textContent = text;
    div.appendChild( p );
    return div;
}


function tooltipsPosition( event, tooltips ){
    tooltips.style.top = event.screenY+"px";
    tooltips.style.left= (event.screenX + 20)+"px";
}


function addNodeEvent(tooltips, svgDoc, nodeId, text, color){
    const svg_node = svgDoc.getElementById(nodeId);
    tooltips_event(tooltips, svg_node, createInformativeNode(text, color) );
}