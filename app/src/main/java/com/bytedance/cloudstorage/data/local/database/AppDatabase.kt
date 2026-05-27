package com.bytedance.cloudstorage.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bytedance.cloudstorage.data.local.dao.FileDao
import com.bytedance.cloudstorage.data.local.entity.FileEntity

/**
 * Room 数据库类
 *
 * entities = [FileEntity::class] 声明该数据库包含的所有表。
 * version = 1 表示数据库版本，后续修改表结构时需要递增并提供 Migration。
 *
 * Room 会在编译期自动生成 AppDatabase_Impl 实现类，
 * 包含建表 SQL 和 DAO 方法的具体实现。
 */
@Database(entities = [FileEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    /** 提供 FileDao 实例，Room 自动生成实现 */
    abstract fun fileDao(): FileDao

    companion object {
        /**
         * 单例模式：保证整个 App 只有一个数据库实例，避免多线程冲突。
         *
         * @Volatile 保证 instance 的修改对所有线程立即可见。
         * synchronized 确保只有一个线程进入创建逻辑。
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE files ADD COLUMN coverUri TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "miniclouddisk.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
