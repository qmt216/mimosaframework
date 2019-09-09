/*
 * Copyright 1999-2101 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mimosaframework.core.json.parser;


/**
 * @author wenshao[szujobs@hotmail.com]
 */
@Deprecated
public class DefaultExtModelParser extends DefaultModelParser {

    public DefaultExtModelParser(String input){
        this(input, ParserConfig.getGlobalInstance());
    }

    public DefaultExtModelParser(String input, ParserConfig mapping){
        super(input, mapping);
    }

    public DefaultExtModelParser(String input, ParserConfig mapping, int features){
        super(input, mapping, features);
    }

    public DefaultExtModelParser(char[] input, int length, ParserConfig mapping, int features){
        super(input, length, mapping, features);
    }

    
}
