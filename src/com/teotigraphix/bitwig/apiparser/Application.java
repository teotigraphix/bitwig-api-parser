////////////////////////////////////////////////////////////////////////////////
// Copyright 2014 Michael Schmalle - Teoti Graphix, LLC
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0 
// 
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, 
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and 
// limitations under the License
// 
// Author: Michael Schmalle, Principal Architect
// mschmalle at teotigraphix dot com
////////////////////////////////////////////////////////////////////////////////

package com.teotigraphix.bitwig.apiparser;

import com.teotigraphix.bitwig.apiparser.core.APIParser;
import com.teotigraphix.bitwig.apiparser.core.ParserConfig;

/**
 * @author Michael Schmalle
 * @since 1.0
 */
public class Application {

    // TODO command line configs
    private static final String SOURCE_DIR = "C:/Users/Teoti/Downloads/_Bitwig/BitwigJavaAPI"
            + "/com/bitwig/base/control_surface/iface";

    private static final String OUTPUT_DIR = "C:/Users/Teoti/Desktop/api";

    /**
     * @param args
     */
    public static void main(String[] args) {
        ParserConfig config = new ParserConfig(SOURCE_DIR, OUTPUT_DIR, "1.0.11");

        APIParser parser = new APIParser(config);
        parser.parse();
    }
}
