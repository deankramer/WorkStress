/*Copyright 2016 WorkStress Experiment
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.ac.mdx.cs.ie.acontextlib;

/**
 * Created by dkram on 09/06/2016.
 */
public interface IOntologyManager {

    public void updateValues(String subject, String predicate, String value);

    public void updateValues(String subject, String predicate, String value, long time);

    public void updateValues(String stream, String subject, String predicate, String value);

    public void updateValues(String stream, String subject, String predicate, String value, long time);
}
