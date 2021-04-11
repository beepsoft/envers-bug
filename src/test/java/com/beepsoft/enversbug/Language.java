package com.beepsoft.enversbug;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Audited
public class Language
{
    @Id
    @GeneratedValue
    public Long mtid;

    public String languageCode;
}
