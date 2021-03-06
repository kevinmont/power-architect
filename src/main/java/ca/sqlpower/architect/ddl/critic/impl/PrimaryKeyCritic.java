/*
 * Copyright (c) 2009, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.architect.ddl.critic.impl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.sqlpower.architect.ddl.critic.CriticAndSettings;
import ca.sqlpower.architect.ddl.critic.Criticism;
import ca.sqlpower.sqlobject.SQLTable;

/**
 * Critic to ensure the primary key is not empty.
 */
public class PrimaryKeyCritic extends CriticAndSettings {
    
    public PrimaryKeyCritic() {
        super(StarterPlatformTypes.GENERIC.getName(), Messages.getString("PrimaryKeyCritic.name"));
    }

    public List<Criticism> criticize(final Object so) {
        if (!(so instanceof SQLTable)) return Collections.emptyList();
        SQLTable t = (SQLTable) so;
        List<Criticism> criticisms = new ArrayList<Criticism>();
        if (t.getPkSize() == 0) {
            criticisms.add(new Criticism(t, "Table has no primary key defined", this));
        }
        return criticisms;
    }

}
