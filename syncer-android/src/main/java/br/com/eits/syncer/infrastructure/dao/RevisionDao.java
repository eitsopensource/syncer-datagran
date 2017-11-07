package br.com.eits.syncer.infrastructure.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import br.com.eits.syncer.Syncer;
import br.com.eits.syncer.application.ApplicationHolder;
import br.com.eits.syncer.domain.entity.Revision;
import br.com.eits.syncer.domain.entity.RevisionType;
import br.com.eits.syncer.domain.service.QueryRevisionService;
import io.requery.android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rodrigo.p.fraga on 03/11/16.
 */
public class RevisionDao<T>
{
    /**
     *
     */
    private static final SQLiteHelper HELPER = new SQLiteHelper( ApplicationHolder.CONTEXT );

    /*-------------------------------------------------------------------
	 * 		 					CONSTRUCTORS
	 *-------------------------------------------------------------------*/
    /**
     *
     */
    public RevisionDao()
    {
    }

    /*-------------------------------------------------------------------
	 * 		 					BEHAVIORS
	 *-------------------------------------------------------------------*/
    /**
     * @param revision
     * @return
     */
    public Revision<T> insertRevision( Revision<T> revision )
    {
        final SQLiteDatabase database = HELPER.getWritableDatabase();

        final ContentValues values = new ContentValues();
        values.put( SQLiteHelper.COLUMN_REVISION_NUMBER, revision.getRevisionNumber() );
        values.put( SQLiteHelper.COLUMN_SYNCED, revision.getSynced() );
        values.put( SQLiteHelper.COLUMN_TYPE, revision.getType().ordinal() );
        values.put( SQLiteHelper.COLUMN_ENTITY, this.toJSON( revision.getEntity() ) );
        values.put( SQLiteHelper.COLUMN_ENTITY_CLASSNAME, revision.getEntityClassName() );
        values.put( SQLiteHelper.COLUMN_ENTITY_ID, revision.getEntityId() );
        values.put( SQLiteHelper.COLUMN_SERVICE_NAME, revision.getServiceName() );

        final long revisionId = database.insert( SQLiteHelper.TABLE_REVISION, null, values );
        revision.setId( revisionId );
        return revision;
    }

    /**
     *
     * @param revisions
     * @return
     */
    public List<Revision<T>> insertRevisions( List< Revision<T>> revisions )
    {
        final SQLiteDatabase database = HELPER.getWritableDatabase();

        for( Revision<T> revision : revisions )
        {
            final ContentValues values = new ContentValues();
            values.put( SQLiteHelper.COLUMN_REVISION_NUMBER, revision.getRevisionNumber() );
            values.put( SQLiteHelper.COLUMN_SYNCED, revision.getSynced() );
            values.put( SQLiteHelper.COLUMN_TYPE, revision.getType().ordinal() );
            values.put( SQLiteHelper.COLUMN_ENTITY, this.toJSON( revision.getEntity() ) );
            values.put( SQLiteHelper.COLUMN_ENTITY_CLASSNAME, revision.getEntityClassName() );
            values.put( SQLiteHelper.COLUMN_ENTITY_ID, revision.getEntityId() );
            values.put( SQLiteHelper.COLUMN_SERVICE_NAME, revision.getServiceName() );

            final long revisionId = database.insert( SQLiteHelper.TABLE_REVISION, null, values );
            revision.setId( revisionId );
        }

        return revisions;
    }

    /**
     *
     * @param id
     * @return
     */
    public Revision<T> findById( long id )
    {
        final SQLiteDatabase database = HELPER.getReadableDatabase();

        final String where = SQLiteHelper.COLUMN_ID + " = ?";
        final Object[] whereArguments = new Object[] { id };

        final Cursor cursor = database.query( SQLiteHelper.TABLE_REVISION, null, where, whereArguments, null, null, null );

        try
        {
            Revision<T> revision = null;
            if ( cursor.moveToFirst() )
            {
                revision = this.fromCursorToRevision( cursor );
            }
            return revision;
        }
        catch ( Exception e )
        {
            throw e;
        }
        finally
        {
            cursor.close();
        }
    }

    /**
     *
     * @param entityId
     * @return
     */
    public Revision<T> findByEntityId( Class<T> className, String entityId )
    {
        final SQLiteDatabase database = HELPER.getReadableDatabase();

        final String where = SQLiteHelper.COLUMN_ENTITY_ID + " = ? AND " + SQLiteHelper.COLUMN_ENTITY_CLASSNAME + " = ?";
        final Object[] whereArguments = new Object[] { entityId, className.getName() };

        final String groupBy = SQLiteHelper.COLUMN_ENTITY_ID;
        final String having = SQLiteHelper.TABLE_REVISION + "." + SQLiteHelper.COLUMN_TYPE + " <> " + RevisionType.REMOVE.ordinal();
        final String orderBy = SQLiteHelper.COLUMN_ID + " DESC";

        final Cursor cursor = database.query( SQLiteHelper.TABLE_REVISION, null, where, whereArguments, groupBy, having, orderBy );

        try
        {
            Revision<T> revision = null;
            if ( cursor.moveToFirst() )
            {
                revision = this.fromCursorToRevision( cursor );
            }
            return revision;
        }
        catch ( Exception e )
        {
            throw e;
        }
        finally
        {
            cursor.close();
        }
    }

    /**
     *
     * @param className
     * @param filters
     * @return
     */
    public List<Revision<T>> listByFilters( Class<T> className, String filters )
    {
        final SQLiteDatabase database = HELPER.getReadableDatabase();

        filters = filters == null ? "" : filters;

        final String tables = SQLiteHelper.TABLE_REVISION+", json_each("+ SQLiteHelper.COLUMN_ENTITY +")";

        //NOT IN nao deixa procurar nos varios niveis do json
        final String where = SQLiteHelper.COLUMN_ENTITY_CLASSNAME + " = ? AND json_each.type NOT IN ( 'object', 'array' ) AND json_each.value LIKE '%" + filters + "%'";
        final Object[] whereArguments = new Object[] { className.getName() };
        final String groupBy = SQLiteHelper.COLUMN_ENTITY_ID;
        final String having = SQLiteHelper.TABLE_REVISION + "." + SQLiteHelper.COLUMN_TYPE + " <> " + RevisionType.REMOVE.ordinal();
        final String orderBy = SQLiteHelper.COLUMN_ID + " DESC";

        final Cursor cursor = database.query( tables, null, where, whereArguments, groupBy, having, orderBy );

        try
        {
            cursor.moveToFirst();

            final List<Revision<T>> revisions = new ArrayList<>();
            while ( !cursor.isAfterLast() )
            {
                revisions.add( this.fromCursorToRevision( cursor ) );
                cursor.moveToNext();
            }
            return revisions;
        }
        catch ( Exception e )
        {
            throw e;
        }
        finally
        {
            cursor.close();
        }
    }

    /**
     *
     * @param queryRevisionService
     * @return
     */
    public List<Revision<T>> listByQueryBuilder( QueryRevisionService queryRevisionService )
    {
        final SQLiteDatabase database = HELPER.getReadableDatabase();

        final String tables = queryRevisionService.getTables();
        final String where = queryRevisionService.getWhere();
        final Object[] whereArguments = queryRevisionService.getWhereArguments().toArray();
        final String groupBy = queryRevisionService.getGroupBy();
        final String having = queryRevisionService.getHaving();
        final String orderBy = queryRevisionService.getOrderBy();
        final String limit = queryRevisionService.getLimit();

        final Cursor cursor = database.queryWithFactory( null, false, tables, null, where, whereArguments, groupBy, having, orderBy, limit );

        try
        {
            cursor.moveToFirst();

            final List<Revision<T>> revisions = new ArrayList<>();
            while ( !cursor.isAfterLast() )
            {
                revisions.add( this.fromCursorToRevision( cursor ) );
                cursor.moveToNext();
            }
            return revisions;
        }
        catch ( Exception e )
        {
            throw e;
        }
        finally
        {
            cursor.close();
        }
    }

    /**
     *
     * @return
     */
    public Revision findByLastRevisionNumber( String serviceName )
    {
        final SQLiteDatabase database = HELPER.getReadableDatabase();

        final String where = SQLiteHelper.COLUMN_SERVICE_NAME + " = ?";
        final Object[] whereArguments = new Object[] { serviceName };
        final String orderBy = SQLiteHelper.COLUMN_REVISION_NUMBER + " DESC";
        final String limit = "1";

        final Cursor cursor = database.queryWithFactory( null, false, SQLiteHelper.TABLE_REVISION, null, where, whereArguments, null, null, orderBy, limit );

        try
        {
            Revision<?> revision = null;
            if ( cursor.moveToFirst() )
            {
                revision = this.fromCursorToRevision(cursor);
            }
            return revision;
        }
        catch ( Exception e )
        {
            throw e;
        }
        finally
        {
            cursor.close();
        }
    }

    /**
     *
     * @return
     */
    public List<Revision<?>> listByUnsyncedByService( String serviceName, int count )
    {
        final SQLiteDatabase database = HELPER.getReadableDatabase();

        final String where = SQLiteHelper.COLUMN_SYNCED + " = ? AND "+SQLiteHelper.COLUMN_SERVICE_NAME + " = ?";
        final Object[] whereArguments = new Object[] { Boolean.FALSE, serviceName };
        final String orderBy = SQLiteHelper.COLUMN_REVISION_NUMBER + " ASC";
        final String limit = String.valueOf(count);

        final Cursor cursor = database.query( SQLiteHelper.TABLE_REVISION, null, where, whereArguments, null, null, orderBy, limit );

        try
        {
            cursor.moveToFirst();

            final List<Revision<?>> revisions = new ArrayList<>();
            while ( !cursor.isAfterLast() )
            {
                revisions.add( this.fromCursorToRevision( cursor ) );
                cursor.moveToNext();
            }
            return revisions;
        }
        catch ( Exception e )
        {
            throw e;
        }
        finally
        {
            cursor.close();
        }
    }

    /**
     *
     */
    public void removeOldRevisions( Revision<T> revision )
    {
        final SQLiteDatabase database = HELPER.getWritableDatabase();
        final String[] whereArguments = new String[] { revision.getEntityId(), revision.getEntityClassName(), revision.getId().toString() };

        final String where = SQLiteHelper.COLUMN_ENTITY_ID + " = ? AND " + SQLiteHelper.COLUMN_ENTITY_CLASSNAME + " = ? AND " + SQLiteHelper.COLUMN_ID + " < ? ";
        database.delete( SQLiteHelper.TABLE_REVISION, where, whereArguments );
    }

    /**
     *
     * @param ids
     */
    public void remove( String... ids )
    {
        final SQLiteDatabase database = HELPER.getWritableDatabase();

        final String where = SQLiteHelper.COLUMN_ID + " IN (" + TextUtils.join( ",", ids ) + ")";
        database.delete( SQLiteHelper.TABLE_REVISION, where, null );
    }

    /**
     *
     * @param cursor
     * @return
     */
    private Revision<T> fromCursorToRevision( Cursor cursor )
    {
        try
        {
            final Class<?> entityClass = Class.forName( cursor.getString(SQLiteHelper.COLUMN_ENTITY_CLASSNAME_INDEX) );
            final Object entity = this.toEntity( cursor.getString(SQLiteHelper.COLUMN_ENTITY_INDEX), entityClass );

            final Revision revision = new Revision(
                    cursor.getLong(SQLiteHelper.COLUMN_ID_INDEX),
                    entity,
                    cursor.getString(SQLiteHelper.COLUMN_ENTITY_ID_INDEX),
                    RevisionType.valueOf(cursor.getInt(SQLiteHelper.COLUMN_TYPE_INDEX)),
                    cursor.getString(SQLiteHelper.COLUMN_SERVICE_NAME_INDEX)
            );

            revision.setRevisionNumber( cursor.getLong( SQLiteHelper.COLUMN_REVISION_NUMBER_INDEX ) );
            revision.setSynced( cursor.getLong( SQLiteHelper.COLUMN_SYNCED_INDEX ) == 1 ? true : false );

            return revision;
        }
        catch ( ClassNotFoundException e )
        {
            e.printStackTrace();
            throw new IllegalStateException("Could not parser the persisted json to an entity instance", e);
        }
    }

    /**
     *
     * @param entity
     * @return
     */
    private String toJSON( Object entity )
    {
        try
        {
            return Syncer.getMapper().writeValueAsString( entity );
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException("Error serializing the entity", e);
        }
    }

    /**
     *
     * @param json
     * @param entityClass
     * @return
     */
    private Object toEntity( String json, Class<?> entityClass )
    {
        try
        {
            return Syncer.getMapper().readValue( json, entityClass );
        }
        catch ( Exception e )
        {
            throw new IllegalArgumentException("Error serializing the entity", e);
        }
    }

}
