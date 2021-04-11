# Demonstrates a bug when having a @ManyToMany relationship with @OrderBy with an ID property of the target entity and trying to load an old revision of the entity.

This repo demonstrates the following error happening:

    java.lang.IllegalArgumentException: org.hibernate.QueryException: could not resolve property: mtid of: com.beepsoft.enversbug.Language_AUD [select new list(ee__, e__) from publication_languages_AUD ee__, com.beepsoft.enversbug.Language_AUD e__ where ee__.originalId.Publication_mtid = :Publication_mtid and e__.originalId.REV.id = (select max(e2__.originalId.REV.id) from com.beepsoft.enversbug.Language_AUD e2__ where e2__.originalId.REV.id <= :revision and e__.originalId.mtid = e2__.originalId.mtid) and ee__.originalId.REV.id = (select max(ee2__.originalId.REV.id) from publication_languages_AUD ee2__ where ee2__.originalId.REV.id <= :revision and ee__.originalId.Publication_mtid = ee2__.originalId.Publication_mtid and ee__.originalId.languages_mtid = ee2__.originalId.languages_mtid) and ee__.REVTYPE != :delrevisiontype and e__.REVTYPE != :delrevisiontype and (ee__.originalId.languages_mtid = e__.originalId.mtid or (ee__.originalId.languages_mtid is null and e__.originalId.mtid is null)) order by e__.mtid]


This is probably caused by having this annotation on the `languages` of `Publication` class:

```java
    @ManyToMany
    @OrderBy("mtid")
    public List<Language> languages;
```

If I use `@OrderBy("languageCode")` instead of the ID field `@OrderBy("mtid")` then it works all right. These very same annotations work all right in `Hibernate 5.0.12.Final` but now fail in Spring 2.4.4 and `Hibernate 5.4.29.Final`

# Reproduce the bug

Run

    ./mvnw clean test
