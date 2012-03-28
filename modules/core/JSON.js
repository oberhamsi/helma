/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2006 Helma Software. All Rights Reserved.
 *
 */


Object.prototype.__noSuchMethod__ = function(id) {
    if (id === 'toJSON') {
        return JSON.stringify.apply(JSON, [this].concat(arguments));
    }
};

Object.prototype.dontEnum("__noSuchMethod__");


/**
 * This method parses a JSON text to produce an object or
 * array. It will return false if there is an error.
 */
String.prototype.parseJSON = function() {
    try {
        return JSON.parse(this);
    } catch (e if e instanceof SyntaxError) {
        return false;
    }
};

String.prototype.dontEnum("parseJSON");
