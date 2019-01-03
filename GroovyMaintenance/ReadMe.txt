How to create a Groovy maintenance file

Write your groovy file, with a name "TimerFailedReArm.groovy". Give the code "TimerFailedReArm" to the user to load it.

Place holder
In the file, you can set some Place Holder. Syntax is {{<key>[;tips:<type>]}}
For example, you can set

List listFlowNodeid= {{ListFlowsNode}};

if you want to give more information to the user to fullfill the label, use the tips:
List listFlowNodeid= {{ListFlowsNode;tips:Give a list of flownode, with the JSON syntax like [123,234]}};

Attention, the place holder key is then the complete string, if you want to reuse the same place holder in different place.
In the key, don't use {{ and ; because they are considered as separateur.
