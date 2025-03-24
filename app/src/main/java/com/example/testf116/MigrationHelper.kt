package com.example.testf116

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration
import io.realm.RealmSchema

class MigrationHelper:RealmMigration {

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        if (oldVersion<1){
            migration0To1(realm.schema)
        }
    }

    private fun migration0To1(schema: RealmSchema){
        schema.get(ExamItem::class.java.simpleName)?.run {
            addField("currentWithLoad",Float::class.java,FieldAttribute.REQUIRED)
            addField("voltageWithLoad",Float::class.java,FieldAttribute.REQUIRED)
            addField("wattWithLoad",Float::class.java,FieldAttribute.REQUIRED)
            addField("powerFactorWithLoad",Float::class.java,FieldAttribute.REQUIRED)
            addField("wattHourWithLoad",Float::class.java,FieldAttribute.REQUIRED)
        }

    }
}