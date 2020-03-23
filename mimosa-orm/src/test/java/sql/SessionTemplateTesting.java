package sql;

import org.junit.Before;
import org.junit.Test;
import org.mimosaframework.orm.*;
import org.mimosaframework.orm.exception.ContextException;

public class SessionTemplateTesting {
    private static SessionTemplate template = null;

    @Before
    public void init() throws ContextException {
        if (template == null) {
            XmlAppContext context = new XmlAppContext(SessionFactoryBuilder.class.getResourceAsStream("/template-mimosa.xml"));
            SessionFactory sessionFactory = context.getSessionFactoryBuilder().build();
            template = new MimosaSessionTemplate();
            ((MimosaSessionTemplate) template).setSessionFactory(sessionFactory);
        }
    }

    @Test
    public void testAlter1() throws Exception {
        SQLAutonomously sqlAutonomously = SQLAutonomously.newInstance(

        );
        AutoResult autoResult = template.getAutonomously(sqlAutonomously);

    }
}