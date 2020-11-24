import org.bonitasoft.engine.bpm.category.Category;
import org.bonitasoft.engine.bpm.category.CategoryCriterion;

List<Category> categories = apiAccessor.getProcessAPI().getCategories(0,
	Integer.MAX_VALUE,
	CategoryCriterion.NAME_ASC);

String categoryName={{Category Name}};

for (Category cat : categories) {
     if (cat.getName().equals( categoryName )) { 
           apiAccessor.getProcessAPI().deleteCategory(cat.getId());
           return "category " + cat.getName() +" deleted";
     }
}
return "category not found";



