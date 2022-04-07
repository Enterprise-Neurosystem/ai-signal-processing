/*******************************************************************************
 * Copyright [2022] [IBM]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.eng.template.yaml;

import java.io.Reader;
import java.io.Writer;

import org.eng.template.ITemplateIO;
import org.eng.template.Template;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class YAMLTemplateIO implements ITemplateIO {

	@Override
	public Template read(Reader reader) {
		Yaml yaml = new Yaml();
		Template md = yaml.loadAs(reader, Template.class);
		return md;
	}

	@Override
	public void write(Writer writer, Template template) {
		DumperOptions options = new DumperOptions();
		options.setPrettyFlow(true);
		Yaml yaml = new Yaml(options);
		yaml.dump(template, writer);
	}

}
