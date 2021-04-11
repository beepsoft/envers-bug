package com.beepsoft.enversbug;

import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.List;

@Entity
@Audited
public class Publication
{
    @Id
    @GeneratedValue
    public Long mtid;

    public String title;

    @ManyToMany
    @OrderBy("mtid")
    public List<Language> languages;
}
